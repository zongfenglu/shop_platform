package com.shopplatform.domain.shop.service.impl;

import com.shopplatform.domain.shop.service.ImpersonateTicketService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

@Service
public class ImpersonateTicketServiceImpl implements ImpersonateTicketService {

    private static final String KEY_PREFIX = "shop:impersonate:";

    private final StringRedisTemplate redisTemplate;

    public ImpersonateTicketServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String issue(Long shopId, Long storeUserId) {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                KEY_PREFIX + ticket,
                shopId + ":" + storeUserId,
                Duration.ofSeconds(TTL_SECONDS));
        return ticket;
    }

    @Override
    public Payload consume(String ticket) {
        if (!StringUtils.hasText(ticket)) {
            return null;
        }
        String raw = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + ticket.trim());
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        int sep = raw.indexOf(':');
        if (sep <= 0 || sep == raw.length() - 1) {
            return null;
        }
        try {
            return new Payload(Long.parseLong(raw.substring(0, sep)), Long.parseLong(raw.substring(sep + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
