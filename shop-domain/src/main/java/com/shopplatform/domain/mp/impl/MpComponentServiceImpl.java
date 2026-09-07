package com.shopplatform.domain.mp.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.mp.MpComponentService;
import com.shopplatform.domain.mp.MpWechatSettings;
import com.shopplatform.domain.mp.entity.MpAuthorizer;
import com.shopplatform.domain.mp.entity.MpComponentTicket;
import com.shopplatform.domain.mp.mapper.MpAuthorizerMapper;
import com.shopplatform.domain.mp.mapper.MpComponentTicketMapper;
import com.shopplatform.domain.mp.wechat.WxMsgCrypt;
import com.shopplatform.domain.mp.wechat.WxOpenPlatformClient;
import com.shopplatform.domain.platform.service.PlatformSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MpComponentServiceImpl extends ServiceImpl<MpComponentTicketMapper, MpComponentTicket>
        implements MpComponentService {

    private static final Logger log = LoggerFactory.getLogger(MpComponentServiceImpl.class);
    private static final long TICKET_ROW_ID = 1L;
    private static final String TOKEN_KEY = "wx:component:access_token";
    private static final Pattern TAG = Pattern.compile("<(\\w+)><!\\[CDATA\\[(.*?)]]></\\1>|<(\\w+)>([^<]*)</\\3>");

    private final PlatformSettingService platformSettingService;
    private final WxOpenPlatformClient wxOpenPlatformClient;
    private final StringRedisTemplate redisTemplate;
    private final MpAuthorizerMapper mpAuthorizerMapper;

    public MpComponentServiceImpl(PlatformSettingService platformSettingService,
                                  WxOpenPlatformClient wxOpenPlatformClient,
                                  StringRedisTemplate redisTemplate,
                                  MpAuthorizerMapper mpAuthorizerMapper) {
        this.platformSettingService = platformSettingService;
        this.wxOpenPlatformClient = wxOpenPlatformClient;
        this.redisTemplate = redisTemplate;
        this.mpAuthorizerMapper = mpAuthorizerMapper;
    }

    @Override
    public MpWechatSettings settings() {
        Map<String, Object> raw = platformSettingService.findValue("wechat");
        return new MpWechatSettings(
                str(raw, "componentAppId"),
                str(raw, "componentSecret"),
                str(raw, "token"),
                str(raw, "encodingAesKey"),
                str(raw, "callbackUrl")
        );
    }

    @Override
    public void requireConfigured() {
        if (!settings().configured()) {
            throw new BusinessException(ErrorCode.WECHAT_COMPONENT_NOT_CONFIGURED);
        }
    }

    @Override
    public String latestTicket() {
        MpComponentTicket row = this.getOne(Wrappers.<MpComponentTicket>lambdaQuery()
                .eq(MpComponentTicket::getId, TICKET_ROW_ID));
        return row == null ? "" : (row.getTicket() == null ? "" : row.getTicket());
    }

    @Override
    public LocalDateTime ticketReceivedTime() {
        MpComponentTicket row = this.getOne(Wrappers.<MpComponentTicket>lambdaQuery()
                .eq(MpComponentTicket::getId, TICKET_ROW_ID));
        return row == null ? null : row.getReceivedTime();
    }

    @Override
    public void saveTicket(String ticket) {
        if (!StringUtils.hasText(ticket)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ticket 为空");
        }
        MpComponentTicket row = this.getOne(Wrappers.<MpComponentTicket>lambdaQuery()
                .eq(MpComponentTicket::getId, TICKET_ROW_ID));
        if (row == null) {
            row = new MpComponentTicket();
            row.setId(TICKET_ROW_ID);
        }
        row.setTicket(ticket.trim());
        row.setReceivedTime(LocalDateTime.now());
        this.saveOrUpdate(row);
        redisTemplate.delete(TOKEN_KEY);
    }

    @Override
    public String handleEventXml(String xml) {
        Map<String, String> tags = parseXml(xml);
        String infoType = tags.getOrDefault("InfoType", "");
        if ("component_verify_ticket".equals(infoType)) {
            saveTicket(tags.getOrDefault("ComponentVerifyTicket", ""));
            return "success";
        }
        if ("unauthorized".equals(infoType)) {
            markUnauthorized(tags.get("AuthorizerAppid"));
            return "success";
        }
        if ("authorized".equals(infoType) || "updateauthorized".equals(infoType)) {
            log.info("收到授权事件 infoType={} appid={}", infoType, tags.get("AuthorizerAppid"));
            return "success";
        }
        log.info("忽略未处理的微信事件 infoType={}", infoType);
        return "success";
    }

    @Override
    public WxMsgCrypt crypt() {
        MpWechatSettings s = settings();
        requireConfigured();
        return new WxMsgCrypt(s.token(), s.encodingAesKey(), s.componentAppId());
    }

    @Override
    public String componentAccessToken() {
        String cached = redisTemplate.opsForValue().get(TOKEN_KEY);
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        return refreshComponentToken().accessToken();
    }

    @Override
    public WxOpenPlatformClient.Token refreshComponentToken() {
        requireConfigured();
        String ticket = latestTicket();
        if (!StringUtils.hasText(ticket)) {
            throw new BusinessException(ErrorCode.WECHAT_TICKET_MISSING);
        }
        MpWechatSettings s = settings();
        WxOpenPlatformClient.Token token = wxOpenPlatformClient.componentToken(
                s.componentAppId(), s.componentSecret(), ticket);
        int ttl = Math.max(60, token.expiresIn() - 200);
        redisTemplate.opsForValue().set(TOKEN_KEY, token.accessToken(), Duration.ofSeconds(ttl));
        return token;
    }

    @Override
    public String createPreAuthCode() {
        MpWechatSettings s = settings();
        requireConfigured();
        return wxOpenPlatformClient.createPreAuthCode(componentAccessToken(), s.componentAppId());
    }

    private void markUnauthorized(String appid) {
        if (!StringUtils.hasText(appid)) {
            return;
        }
        MpAuthorizer row = mpAuthorizerMapper.selectOne(Wrappers.<MpAuthorizer>lambdaQuery()
                .eq(MpAuthorizer::getAppid, appid));
        if (row == null) {
            return;
        }
        row.setAuthStatus("unauthorized");
        row.setUnauthorizedTime(LocalDateTime.now());
        mpAuthorizerMapper.updateById(row);
    }

    static Map<String, String> parseXml(String xml) {
        java.util.LinkedHashMap<String, String> map = new java.util.LinkedHashMap<>();
        if (xml == null) {
            return map;
        }
        Matcher m = TAG.matcher(xml);
        while (m.find()) {
            if (m.group(1) != null) {
                map.put(m.group(1), m.group(2));
            } else {
                map.put(m.group(3), m.group(4));
            }
        }
        return map;
    }

    private static String str(Map<String, Object> raw, String key) {
        Object v = raw == null ? null : raw.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }
}
