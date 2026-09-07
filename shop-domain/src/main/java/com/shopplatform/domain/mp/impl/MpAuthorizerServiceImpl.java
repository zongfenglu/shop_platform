package com.shopplatform.domain.mp.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.mp.MpAuthorizerService;
import com.shopplatform.domain.mp.MpComponentService;
import com.shopplatform.domain.mp.MpWechatSettings;
import com.shopplatform.domain.mp.entity.MpAuthorizer;
import com.shopplatform.domain.mp.mapper.MpAuthorizerMapper;
import com.shopplatform.domain.mp.wechat.WxOpenPlatformClient;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.domain.shop.service.PackageFeatureChecker;
import com.shopplatform.domain.shop.service.ShopDomainService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.framework.crypto.AesGcmEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MpAuthorizerServiceImpl extends ServiceImpl<MpAuthorizerMapper, MpAuthorizer>
        implements MpAuthorizerService {

    private final MpComponentService mpComponentService;
    private final WxOpenPlatformClient wxOpenPlatformClient;
    private final AesGcmEncryptor aesGcmEncryptor;
    private final PackageFeatureChecker packageFeatureChecker;
    private final ShopService shopService;
    private final ShopDomainService shopDomainService;
    private final ObjectMapper objectMapper;
    private final String apiPublicUrl;
    private final String h5PublicUrl;
    private final String platformBaseDomain;

    public MpAuthorizerServiceImpl(MpComponentService mpComponentService,
                                   WxOpenPlatformClient wxOpenPlatformClient,
                                   AesGcmEncryptor aesGcmEncryptor,
                                   PackageFeatureChecker packageFeatureChecker,
                                   ShopService shopService,
                                   ShopDomainService shopDomainService,
                                   ObjectMapper objectMapper,
                                   @Value("${shop.client.public-url:http://localhost:8083}") String apiPublicUrl,
                                   @Value("${shop.h5-public-url:http://localhost:5175}") String h5PublicUrl,
                                   @Value("${shop.platform.base-domain:shop.com}") String platformBaseDomain) {
        this.mpComponentService = mpComponentService;
        this.wxOpenPlatformClient = wxOpenPlatformClient;
        this.aesGcmEncryptor = aesGcmEncryptor;
        this.packageFeatureChecker = packageFeatureChecker;
        this.shopService = shopService;
        this.shopDomainService = shopDomainService;
        this.objectMapper = objectMapper;
        this.apiPublicUrl = apiPublicUrl;
        this.h5PublicUrl = h5PublicUrl;
        this.platformBaseDomain = platformBaseDomain;
    }

    @Override
    public List<MpAuthorizer> listByShop(Long shopId) {
        return this.list(Wrappers.<MpAuthorizer>lambdaQuery()
                .eq(MpAuthorizer::getShopId, shopId)
                .orderByAsc(MpAuthorizer::getAppType));
    }

    @Override
    public List<MpAuthorizer> listAll() {
        return this.list(Wrappers.<MpAuthorizer>lambdaQuery()
                .orderByDesc(MpAuthorizer::getUpdateTime));
    }

    @Override
    public MpAuthorizer findByShopAndType(Long shopId, String appType) {
        return this.getOne(Wrappers.<MpAuthorizer>lambdaQuery()
                .eq(MpAuthorizer::getShopId, shopId)
                .eq(MpAuthorizer::getAppType, normalizeType(appType)));
    }

    @Override
    public Long findShopIdByAppId(String appId) {
        if (!StringUtils.hasText(appId)) {
            return null;
        }
        MpAuthorizer row = this.getOne(Wrappers.<MpAuthorizer>lambdaQuery()
                .eq(MpAuthorizer::getAppid, appId.trim())
                .eq(MpAuthorizer::getAuthStatus, "authorized"));
        return row == null ? null : row.getShopId();
    }

    @Override
    public MpAuthorizer saveSelf(Long shopId, String appType, String appId, String appSecret) {
        String type = normalizeType(appType);
        if (!StringUtils.hasText(appId) || !appId.startsWith("wx")) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请填写有效的 AppID");
        }
        MpAuthorizer other = this.getOne(Wrappers.<MpAuthorizer>lambdaQuery()
                .eq(MpAuthorizer::getAppid, appId.trim())
                .ne(MpAuthorizer::getShopId, shopId));
        if (other != null) {
            throw new BusinessException(ErrorCode.MP_AUTHORIZER_EXISTS, "该 AppID 已被其他商城使用");
        }
        MpAuthorizer row = findByShopAndType(shopId, type);
        if (row == null) {
            row = new MpAuthorizer();
            row.setShopId(shopId);
            row.setAppType(type);
        }
        row.setAuthMode("self");
        row.setAppid(appId.trim());
        if (StringUtils.hasText(appSecret) && !"********".equals(appSecret)) {
            row.setAppSecretEncrypted(aesGcmEncryptor.encrypt(appSecret.trim()));
        } else if (row.getAppSecretEncrypted() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请填写 AppSecret");
        }
        row.setRefreshTokenEncrypted(null);
        row.setAuthStatus("authorized");
        row.setAuthorizedTime(LocalDateTime.now());
        row.setUnauthorizedTime(null);
        this.saveOrUpdate(row);
        return findByShopAndType(shopId, type);
    }

    @Override
    public void unbind(Long shopId, String appType) {
        MpAuthorizer row = findByShopAndType(shopId, appType);
        if (row == null) {
            throw new BusinessException(ErrorCode.MP_AUTHORIZER_NOT_FOUND);
        }
        this.remove(Wrappers.<MpAuthorizer>lambdaQuery().eq(MpAuthorizer::getId, row.getId()));
    }

    @Override
    public AuthUrl startHostedAuth(Long shopId, String appType) {
        packageFeatureChecker.requireMenu(shopId, "mp.authorize");
        mpComponentService.requireConfigured();
        String type = normalizeType(appType);
        String pre = mpComponentService.createPreAuthCode();
        MpWechatSettings s = mpComponentService.settings();
        String redirect = s.authCallbackUrl()
                + "?shopId=" + shopId
                + "&appType=" + type;
        String url = "https://mp.weixin.qq.com/cgi-bin/componentloginpage"
                + "?component_appid=" + url(s.componentAppId())
                + "&pre_auth_code=" + url(pre)
                + "&redirect_uri=" + url(redirect)
                + "&auth_type=" + ("official".equals(type) ? "1" : "2");
        return new AuthUrl(url, pre, redirect);
    }

    @Override
    public MpAuthorizer completeHostedAuth(Long shopId, String appType, String authCode) {
        if (!StringUtils.hasText(authCode)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "缺少 auth_code");
        }
        packageFeatureChecker.requireMenu(shopId, "mp.authorize");
        MpWechatSettings s = mpComponentService.settings();
        mpComponentService.requireConfigured();
        String type = normalizeType(appType);
        WxOpenPlatformClient.Authorization auth = wxOpenPlatformClient.queryAuth(
                mpComponentService.componentAccessToken(), s.componentAppId(), authCode.trim());
        MpAuthorizer row = findByShopAndType(shopId, type);
        if (row == null) {
            row = new MpAuthorizer();
            row.setShopId(shopId);
            row.setAppType(type);
        }
        row.setAuthMode("hosted");
        row.setAppid(auth.authorizerAppId());
        row.setAppSecretEncrypted(null);
        row.setRefreshTokenEncrypted(aesGcmEncryptor.encrypt(auth.authorizerRefreshToken()));
        row.setFuncInfo(auth.funcInfoJson());
        row.setAuthStatus("authorized");
        row.setAuthorizedTime(LocalDateTime.now());
        row.setUnauthorizedTime(null);
        try {
            var info = wxOpenPlatformClient.getAuthorizerInfo(
                    mpComponentService.componentAccessToken(), s.componentAppId(), auth.authorizerAppId());
            row.setNickName(info.nickName());
        } catch (RuntimeException e) {
            // 授权已成功，资料拉取失败不回滚
        }
        this.saveOrUpdate(row);
        return findByShopAndType(shopId, type);
    }

    @Override
    public Map<String, Object> extJson(Long shopId, String appType) {
        Shop shop = shopService.getOne(Wrappers.<Shop>lambdaQuery().eq(Shop::getId, shopId));
        if (shop == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        MpAuthorizer auth = findByShopAndType(shopId, appType == null ? "mini" : appType);
        ShopDomain sub = shopDomainService.listByShopId(shopId).stream()
                .filter(d -> "sub".equals(d.getType()))
                .findFirst()
                .orElse(null);
        String host = sub != null ? sub.getDomain() : shop.getCode() + "." + platformBaseDomain;
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("shopId", shopId);
        ext.put("shopCode", shop.getCode());
        ext.put("apiBase", apiPublicUrl);
        ext.put("h5Base", h5PublicUrl);
        ext.put("host", host);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("extEnable", true);
        root.put("extAppid", auth == null ? "" : auth.getAppid());
        root.put("directCommit", false);
        root.put("ext", ext);
        try {
            objectMapper.writeValueAsString(root);
        } catch (Exception ignored) {
            // 仅校验可序列化
        }
        return root;
    }

    private static String normalizeType(String appType) {
        if ("official".equals(appType) || "mp".equals(appType) || "wechat".equals(appType)) {
            return "official";
        }
        return "mini";
    }

    private static String url(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }
}
