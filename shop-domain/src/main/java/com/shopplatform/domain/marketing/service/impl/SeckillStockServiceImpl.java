package com.shopplatform.domain.marketing.service.impl;

import com.shopplatform.domain.marketing.service.SeckillStockService;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeckillStockServiceImpl implements SeckillStockService {

    private static final String PRE_DEDUCT_LUA =
            "local qty = tonumber(ARGV[1])\n" +
            "local limit = tonumber(ARGV[2])\n" +
            "local bought = tonumber(redis.call('GET', KEYS[2]) or '0')\n" +
            "if limit > 0 and bought + qty > limit then return -2 end\n" +
            "local stock = tonumber(redis.call('GET', KEYS[1]) or '0')\n" +
            "if stock < qty then return -1 end\n" +
            "redis.call('DECRBY', KEYS[1], qty)\n" +
            "redis.call('INCRBY', KEYS[2], qty)\n" +
            "return 1";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> preDeductScript;

    public SeckillStockServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.preDeductScript = new DefaultRedisScript<>();
        this.preDeductScript.setScriptText(PRE_DEDUCT_LUA);
        this.preDeductScript.setResultType(Long.class);
    }

    private String stockKey(Long activeId, Long skuId) {
        return "t:" + TenantContext.getRequired() + ":seckill:stock:" + activeId + ":" + skuId;
    }

    private String boughtKey(Long activeId, Long skuId, Long userId) {
        return "t:" + TenantContext.getRequired() + ":seckill:bought:" + activeId + ":" + skuId + ":" + userId;
    }

    @Override
    public int preDeduct(Long userId, Long activeId, Long skuId, int qty, int limitPerUser) {
        Long r = redisTemplate.execute(preDeductScript,
                List.of(stockKey(activeId, skuId), boughtKey(activeId, skuId, userId)),
                String.valueOf(qty), String.valueOf(limitPerUser));
        return r == null ? STOCK_INSUFFICIENT : r.intValue();
    }

    @Override
    public void rollback(Long userId, Long activeId, Long skuId, int qty) {
        redisTemplate.opsForValue().increment(stockKey(activeId, skuId), qty);
        redisTemplate.opsForValue().increment(boughtKey(activeId, skuId, userId), -qty);
    }

    @Override
    public void initStock(Long activeId, Long skuId, int seckillNum) {
        redisTemplate.opsForValue().set(stockKey(activeId, skuId), String.valueOf(Math.max(0, seckillNum)));
    }

    @Override
    public void reconcile(Long activeId, Long skuId, int seckillNum, int sold) {
        int stock = Math.max(0, seckillNum - sold);
        redisTemplate.opsForValue().set(stockKey(activeId, skuId), String.valueOf(stock));
    }

    @Override
    public void evictStock(Long activeId, Long skuId) {
        redisTemplate.delete(stockKey(activeId, skuId));
    }

    @Override
    public Integer getStock(Long activeId, Long skuId) {
        String v = redisTemplate.opsForValue().get(stockKey(activeId, skuId));
        if (v == null) {
            return null;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
