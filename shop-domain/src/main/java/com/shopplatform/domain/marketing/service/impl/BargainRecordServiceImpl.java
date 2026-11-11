package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.marketing.entity.BargainRecord;
import com.shopplatform.domain.marketing.mapper.BargainRecordMapper;
import com.shopplatform.domain.marketing.service.BargainRecordService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BargainRecordServiceImpl extends ServiceImpl<BargainRecordMapper, BargainRecord> implements BargainRecordService {

    @Override
    public BargainRecord startBargain(Long activeId, Long userId, BigDecimal originPrice, int validHours) {
        BargainRecord existing = findByActiveAndUser(activeId, userId);
        if (existing != null) {
            return reuseOrReturn(existing, originPrice, validHours);
        }
        BargainRecord r = new BargainRecord();
        r.setActiveId(activeId);
        r.setUserId(userId);
        r.setCurrentPrice(originPrice);
        r.setHelpCount(0);
        r.setStatus("ongoing");
        r.setExpireTime(LocalDateTime.now().plusHours(validHours));
        try {
            save(r);
            return r;
        } catch (DataIntegrityViolationException e) {
            BargainRecord raced = findByActiveAndUser(activeId, userId);
            if (raced != null) {
                return reuseOrReturn(raced, originPrice, validHours);
            }
            throw e;
        }
    }

    /** uk_active_user 每人每活动只有一行：过期后复用重开，进行中/已成交直接返回。 */
    private BargainRecord reuseOrReturn(BargainRecord existing, BigDecimal originPrice, int validHours) {
        if ("expired".equals(existing.getStatus())) {
            existing.setCurrentPrice(originPrice);
            existing.setHelpCount(0);
            existing.setStatus("ongoing");
            existing.setExpireTime(LocalDateTime.now().plusHours(validHours));
            existing.setOrderId(null);
            updateById(existing);
        }
        return existing;
    }

    @Override
    public BargainRecord helpCut(Long recordId, BigDecimal cutAmount, BigDecimal floorPrice) {
        int affected = baseMapper.helpCut(recordId, cutAmount, floorPrice);
        if (affected == 0) {
            return null;
        }
        BargainRecord r = getByIdWithTenant(recordId);
        // 砍到底价或达助力上限（help_limit 由调用方在 controller 层校验）则置 done
        if ("ongoing".equals(r.getStatus()) && r.getCurrentPrice().compareTo(floorPrice) <= 0) {
            r.setStatus("done");
            updateById(r);
        }
        return r;
    }

    @Override
    public BargainRecord findByActiveAndUser(Long activeId, Long userId) {
        return getOne(new LambdaQueryWrapper<BargainRecord>()
                .eq(BargainRecord::getActiveId, activeId)
                .eq(BargainRecord::getUserId, userId), false);
    }

    @Override
    public BargainRecord findByOrderId(Long orderId) {
        if (orderId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<BargainRecord>()
                .eq(BargainRecord::getOrderId, orderId), false);
    }

    @Override
    public List<BargainRecord> listInProgress(Long userId) {
        return list(new LambdaQueryWrapper<BargainRecord>()
                .eq(BargainRecord::getUserId, userId)
                .in(BargainRecord::getStatus, List.of("ongoing", "done"))
                .gt(BargainRecord::getExpireTime, LocalDateTime.now())
                .orderByDesc(BargainRecord::getCreateTime));
    }

    @Override
    public List<BargainRecord> listExpired(LocalDateTime now) {
        return list(new LambdaQueryWrapper<BargainRecord>()
                .in(BargainRecord::getStatus, List.of("ongoing", "done"))
                .lt(BargainRecord::getExpireTime, now));
    }

    @Override
    public void markExpired(Long recordId) {
        BargainRecord r = getByIdWithTenant(recordId);
        if ("ongoing".equals(r.getStatus()) || "done".equals(r.getStatus())) {
            r.setStatus("expired");
            updateById(r);
        }
    }

    @Override
    public void markOrdered(Long recordId, Long orderId) {
        BargainRecord r = getByIdWithTenant(recordId);
        if ("ongoing".equals(r.getStatus()) || "done".equals(r.getStatus())) {
            r.setStatus("ordered");
            r.setOrderId(orderId);
            updateById(r);
        }
    }
}
