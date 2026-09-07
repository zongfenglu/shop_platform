package com.shopplatform.domain.ops.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.ops.CacheOpsService;
import com.shopplatform.framework.cache.RedisKeyAdmin;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CacheOpsServiceImpl implements CacheOpsService {

    private final RedisKeyAdmin redisKeyAdmin;

    public CacheOpsServiceImpl(RedisKeyAdmin redisKeyAdmin) {
        this.redisKeyAdmin = redisKeyAdmin;
    }

    @Override
    public Snapshot snapshot() {
        RedisKeyAdmin.MemoryInfo mem = redisKeyAdmin.memory();
        return new Snapshot(
                redisKeyAdmin.count("t:*"),
                mem.usedMemory(),
                mem.maxMemory(),
                mem.hitRate()
        );
    }

    @Override
    public long flush(String scope, Long shopId) {
        if (!StringUtils.hasText(scope)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请选择清除范围");
        }
        String pattern = patternOf(scope.trim(), shopId);
        return redisKeyAdmin.deleteByPattern(pattern);
    }

    static String patternOf(String scope, Long shopId) {
        return switch (scope) {
            case "tenant" -> {
                if (shopId == null) {
                    throw new BusinessException(ErrorCode.PARAM_INVALID, "请选择要清除的商城");
                }
                yield "t:" + shopId + ":*";
            }
            case "goods" -> "t:*:goods:*";
            case "diy" -> "t:*:diy:*";
            case "seckill" -> "t:*:seckill:*";
            case "all" -> "t:*";
            default -> throw new BusinessException(ErrorCode.PARAM_INVALID, "不支持的缓存范围");
        };
    }
}
