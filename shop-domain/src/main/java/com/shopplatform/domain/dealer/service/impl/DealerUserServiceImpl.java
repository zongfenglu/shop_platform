package com.shopplatform.domain.dealer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.dealer.entity.DealerSetting;
import com.shopplatform.domain.dealer.entity.DealerUser;
import com.shopplatform.domain.dealer.mapper.DealerUserMapper;
import com.shopplatform.domain.dealer.service.DealerSettingService;
import com.shopplatform.domain.dealer.service.DealerUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DealerUserServiceImpl extends ServiceImpl<DealerUserMapper, DealerUser> implements DealerUserService {

    private final DealerSettingService dealerSettingService;

    public DealerUserServiceImpl(DealerSettingService dealerSettingService) {
        this.dealerSettingService = dealerSettingService;
    }

    @Override
    @Transactional
    public DealerUser apply(Long userId) {
        DealerSetting setting = dealerSettingService.getOrCreate();
        if (setting.getIsEnable() == null || setting.getIsEnable() == 0) {
            throw new BusinessException(ErrorCode.DEALER_DISABLED, "分销功能未开启");
        }

        // 检查是否已有申请
        DealerUser existing = getOne(new LambdaQueryWrapper<DealerUser>().eq(DealerUser::getUserId, userId));
        if (existing != null) {
            if ("active".equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.DEALER_ALREADY_APPLIED, "已是分销商");
            }
            if ("applying".equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.DEALER_ALREADY_APPLIED, "审核中，请耐心等待");
            }
            // rejected/disabled → 重新申请
            existing.setStatus("applying");
            existing.setApplyTime(LocalDateTime.now());
            updateById(existing);
            return existing;
        }

        DealerUser du = new DealerUser();
        du.setUserId(userId);
        du.setStatus("applying");
        du.setTotalCommission(BigDecimal.ZERO);
        du.setAvailableCommission(BigDecimal.ZERO);
        du.setFrozenCommission(BigDecimal.ZERO);
        du.setApplyTime(LocalDateTime.now());
        save(du);
        return du;
    }

    @Override
    @Transactional
    public DealerUser approve(Long id) {
        DealerUser du = getByIdWithTenant(id);
        if (du == null || !"applying".equals(du.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "仅可审批申请中的分销商");
        }
        du.setStatus("active");
        updateById(du);
        return du;
    }

    @Override
    @Transactional
    public DealerUser reject(Long id) {
        DealerUser du = getByIdWithTenant(id);
        if (du == null || !"applying".equals(du.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "仅可拒绝申请中的分销商");
        }
        du.setStatus("rejected");
        updateById(du);
        return du;
    }

    @Override
    @Transactional
    public DealerUser disable(Long id) {
        DealerUser du = getByIdWithTenant(id);
        if (du == null) throw new BusinessException(ErrorCode.NOT_FOUND, "分销商不存在");
        du.setStatus("disabled");
        updateById(du);
        return du;
    }

    @Override
    public List<DealerUser> listByShop(String status, String keyword) {
        LambdaQueryWrapper<DealerUser> wrapper = new LambdaQueryWrapper<DealerUser>()
                .orderByDesc(DealerUser::getId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DealerUser::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(DealerUser::getRealName, keyword).or().like(DealerUser::getMobile, keyword));
        }
        return list(wrapper);
    }

    @Override
    public DealerUser getMyDealer(Long userId) {
        return getOne(new LambdaQueryWrapper<DealerUser>().eq(DealerUser::getUserId, userId));
    }

    @Override
    public List<DealerUser> listMyTeam(Long userId) {
        DealerUser me = getMyDealer(userId);
        if (me == null) return List.of();
        return list(new LambdaQueryWrapper<DealerUser>().eq(DealerUser::getParentId, me.getId()));
    }
}
