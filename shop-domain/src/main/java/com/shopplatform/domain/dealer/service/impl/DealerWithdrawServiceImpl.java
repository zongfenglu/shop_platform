package com.shopplatform.domain.dealer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.dealer.entity.DealerSetting;
import com.shopplatform.domain.dealer.entity.DealerUser;
import com.shopplatform.domain.dealer.entity.DealerWithdraw;
import com.shopplatform.domain.dealer.mapper.DealerWithdrawMapper;
import com.shopplatform.domain.dealer.service.DealerSettingService;
import com.shopplatform.domain.dealer.service.DealerUserService;
import com.shopplatform.domain.dealer.service.DealerWithdrawService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DealerWithdrawServiceImpl extends ServiceImpl<DealerWithdrawMapper, DealerWithdraw> implements DealerWithdrawService {

    private final DealerSettingService dealerSettingService;
    private final DealerUserService dealerUserService;

    public DealerWithdrawServiceImpl(DealerSettingService dealerSettingService, DealerUserService dealerUserService) {
        this.dealerSettingService = dealerSettingService;
        this.dealerUserService = dealerUserService;
    }

    @Override
    @Transactional
    public DealerWithdraw apply(Long userId, BigDecimal amount, String method, String accountInfo) {
        DealerUser du = dealerUserService.getMyDealer(userId);
        if (du == null || !"active".equals(du.getStatus())) {
            throw new BusinessException(ErrorCode.DEALER_NOT_FOUND, "非分销商无法提现");
        }

        DealerSetting setting = dealerSettingService.getOrCreate();
        BigDecimal min = setting.getMinWithdraw() != null ? setting.getMinWithdraw() : BigDecimal.TEN;
        if (amount.compareTo(min) < 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "最低提现金额 ¥" + min);
        }
        if (amount.compareTo(du.getAvailableCommission()) > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "可提现金额不足");
        }

        // 冻结可提现佣金
        du.setAvailableCommission(du.getAvailableCommission().subtract(amount));
        du.setFrozenCommission(du.getFrozenCommission().add(amount));
        dealerUserService.updateById(du);

        DealerWithdraw dw = new DealerWithdraw();
        dw.setDealerUserId(du.getId());
        dw.setUserId(userId);
        dw.setAmount(amount);
        dw.setMethod(method != null ? method : "wechat");
        dw.setAccountInfo(accountInfo);
        dw.setStatus("applying");
        dw.setApplyTime(LocalDateTime.now());
        save(dw);
        return dw;
    }

    @Override
    @Transactional
    public DealerWithdraw approve(Long id, String remark) {
        DealerWithdraw dw = getByIdWithTenant(id);
        if (dw == null || !"applying".equals(dw.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "仅可审核申请中的提现单");
        }
        dw.setStatus("approved");
        dw.setRemark(remark);
        dw.setReviewTime(LocalDateTime.now());
        updateById(dw);
        // 实际打款由外部系统/手动完成，这里只做状态变更
        return dw;
    }

    @Override
    @Transactional
    public DealerWithdraw reject(Long id, String remark) {
        DealerWithdraw dw = getByIdWithTenant(id);
        if (dw == null || !"applying".equals(dw.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "仅可审核申请中的提现单");
        }
        dw.setStatus("rejected");
        dw.setRemark(remark);
        dw.setReviewTime(LocalDateTime.now());
        updateById(dw);

        // 解冻佣金
        DealerUser du = dealerUserService.getByIdWithTenant(dw.getDealerUserId());
        if (du != null) {
            du.setAvailableCommission(du.getAvailableCommission().add(dw.getAmount()));
            du.setFrozenCommission(du.getFrozenCommission().subtract(dw.getAmount()));
            dealerUserService.updateById(du);
        }
        return dw;
    }

    @Override
    public List<DealerWithdraw> listByShop(String status) {
        LambdaQueryWrapper<DealerWithdraw> wrapper = new LambdaQueryWrapper<DealerWithdraw>()
                .orderByDesc(DealerWithdraw::getId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DealerWithdraw::getStatus, status);
        }
        return list(wrapper);
    }

    @Override
    public List<DealerWithdraw> listByDealer(Long dealerUserId) {
        return list(new LambdaQueryWrapper<DealerWithdraw>()
                .eq(DealerWithdraw::getDealerUserId, dealerUserId)
                .orderByDesc(DealerWithdraw::getId));
    }
}
