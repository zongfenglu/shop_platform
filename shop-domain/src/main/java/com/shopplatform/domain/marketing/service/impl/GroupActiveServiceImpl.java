package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.marketing.entity.GroupActive;
import com.shopplatform.domain.marketing.mapper.GroupActiveMapper;
import com.shopplatform.domain.marketing.service.GroupActiveService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroupActiveServiceImpl extends ServiceImpl<GroupActiveMapper, GroupActive> implements GroupActiveService {

    private final ObjectMapper objectMapper;

    public GroupActiveServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<GroupActive> listOnSale() {
        LocalDateTime now = LocalDateTime.now();
        return list(new LambdaQueryWrapper<GroupActive>()
                .eq(GroupActive::getStatus, "on")
                .le(GroupActive::getStartTime, now)
                .ge(GroupActive::getEndTime, now)
                .orderByDesc(GroupActive::getId));
    }

    @Override
    public List<GroupActive> listAll() {
        return list(new LambdaQueryWrapper<GroupActive>()
                .orderByDesc(GroupActive::getId));
    }

    @Override
    public GroupActive getByCompatibleIdWithTenant(Long requestedId) {
        if (requestedId == null) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND, "拼团活动不存在");
        }
        List<GroupActive> activities = listAll();
        GroupActive exact = activities.stream()
                .filter(active -> requestedId.equals(active.getId()))
                .findFirst()
                .orElse(null);
        if (exact != null) {
            return exact;
        }

        List<GroupActive> compatible = activities.stream()
                .filter(active -> active.getId() != null
                        && active.getId().doubleValue() == requestedId.doubleValue())
                .toList();
        if (compatible.size() == 1) {
            return compatible.get(0);
        }
        throw new BusinessException(ErrorCode.GROUP_NOT_FOUND, "拼团活动不存在");
    }

    @Override
    public Map<Long, BigDecimal> parseGroupPrice(GroupActive active) {
        if (active.getGroupPrice() == null || active.getGroupPrice().isBlank()) {
            return Map.of();
        }
        try {
            // JSON 形如 {"10001": 80.00}，key 为字符串形式的 skuId
            Map<String, BigDecimal> raw = objectMapper.readValue(active.getGroupPrice(),
                    new TypeReference<Map<String, BigDecimal>>() {});
            Map<Long, BigDecimal> result = new LinkedHashMap<>();
            raw.forEach((k, v) -> result.put(Long.valueOf(k), v));
            return result;
        } catch (Exception e) {
            return Map.of();
        }
    }
}
