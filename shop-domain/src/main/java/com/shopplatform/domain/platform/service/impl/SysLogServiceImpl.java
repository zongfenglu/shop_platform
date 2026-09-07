package com.shopplatform.domain.platform.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.platform.entity.SysLog;
import com.shopplatform.domain.platform.mapper.SysLogMapper;
import com.shopplatform.domain.platform.service.SysLogService;
import com.shopplatform.framework.security.LoginUserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    private static final Logger log = LoggerFactory.getLogger(SysLogServiceImpl.class);

    @Override
    public void record(Long shopId,
                       int operatorType,
                       Long operatorId,
                       String operatorName,
                       boolean byPlatform,
                       String action,
                       String description,
                       String ip) {
        SysLog row = new SysLog();
        row.setShopId(shopId);
        row.setOperatorType(operatorType);
        row.setOperatorId(operatorId == null ? 0L : operatorId);
        row.setOperatorName(operatorName);
        row.setByPlatform(byPlatform);
        row.setAction(action);
        row.setDescription(description);
        row.setIp(ip);
        try {
            this.save(row);
        } catch (RuntimeException e) {
            log.error("写入审计日志失败 action={}", action, e);
        }
    }

    @Override
    public void record(Long shopId, String action, String description, String ip) {
        LoginUserContext.LoginUser user = LoginUserContext.get();
        if (user == null) {
            record(shopId, 0, 0L, "anonymous", false, action, description, ip);
            return;
        }
        int type = user.shopId() == null ? 1 : 2;
        record(shopId, type, user.userId(), user.username(), user.platformImpersonation(), action, description, ip);
    }

    @Override
    public IPage<SysLog> page(String kind, int pageNum, int pageSize) {
        var wrapper = Wrappers.<SysLog>lambdaQuery().orderByDesc(SysLog::getCreateTime);
        if ("login".equals(kind)) {
            wrapper.eq(SysLog::getAction, "login");
        } else if ("op".equals(kind) || !StringUtils.hasText(kind)) {
            wrapper.ne(SysLog::getAction, "login");
        }
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }
}
