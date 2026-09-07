package com.shopplatform.domain.dealer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.dealer.entity.DealerOrder;
import com.shopplatform.domain.dealer.entity.DealerSetting;
import com.shopplatform.domain.dealer.entity.DealerUser;
import com.shopplatform.domain.dealer.mapper.DealerOrderMapper;
import com.shopplatform.domain.dealer.service.DealerOrderService;
import com.shopplatform.domain.dealer.service.DealerSettingService;
import com.shopplatform.domain.dealer.service.DealerUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DealerOrderServiceImpl extends ServiceImpl<DealerOrderMapper, DealerOrder> implements DealerOrderService {

    private static final Logger log = LoggerFactory.getLogger(DealerOrderServiceImpl.class);

    private final DealerSettingService dealerSettingService;
    private final DealerUserService dealerUserService;

    public DealerOrderServiceImpl(DealerSettingService dealerSettingService, DealerUserService dealerUserService) {
        this.dealerSettingService = dealerSettingService;
        this.dealerUserService = dealerUserService;
    }

    @Override
    @Transactional
    public void createPending(Long orderId, Long userId, BigDecimal orderTotal) {
        DealerSetting setting = dealerSettingService.getOrCreate();
        if (setting.getIsEnable() == null || setting.getIsEnable() == 0) {
            return; // 分销未开启，不生成佣金
        }

        // 查找该用户的上级分销商（通过 Member 的 inviter/dealer 推荐关系）
        // 当前简化：通过 dealer_user 中的 parent_id 反向查
        // 实际场景中，需要在用户下单时传入推荐人 dealerId
        // 这里先查该用户自己是否是分销商的下级（通过 Member 或其他方式）
        // 简化实现：查找该用户是否关联了某个分销商（实际应由推荐链接参数决定）
        DealerUser dealer = dealerUserService.getMyDealer(userId);
        if (dealer == null || !"active".equals(dealer.getStatus())) {
            // 该用户不是分销商，也不在分销链中，跳过
            log.debug("User {} is not a dealer, skip commission", userId);
            return;
        }
        // 如果用户自己是分销商，查他的上级
        if (dealer.getParentId() != null) {
            DealerUser parent = dealerUserService.getByIdWithTenant(dealer.getParentId());
            if (parent != null && "active".equals(parent.getStatus())) {
                // 上级获得佣金
                BigDecimal rate = setting.getCommissionRate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                BigDecimal amount = orderTotal.multiply(rate).setScale(2, RoundingMode.HALF_DOWN);

                DealerOrder dOrder = new DealerOrder();
                dOrder.setOrderId(orderId);
                dOrder.setDealerUserId(parent.getId());
                dOrder.setOrderTotal(orderTotal);
                dOrder.setCommissionRate(setting.getCommissionRate());
                dOrder.setCommissionAmount(amount);
                dOrder.setStatus("pending");
                save(dOrder);
                return;
            }
        }

        // 如果用户没有上级分销商，看他自己是不是分销商，给他自己计佣金
        if ("active".equals(dealer.getStatus())) {
            BigDecimal rate = setting.getCommissionRate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal amount = orderTotal.multiply(rate).setScale(2, RoundingMode.HALF_DOWN);

            DealerOrder dOrder = new DealerOrder();
            dOrder.setOrderId(orderId);
            dOrder.setDealerUserId(dealer.getId());
            dOrder.setOrderTotal(orderTotal);
            dOrder.setCommissionRate(setting.getCommissionRate());
            dOrder.setCommissionAmount(amount);
            dOrder.setStatus("pending");
            save(dOrder);
        }
    }

    @Override
    public List<DealerOrder> listByDealer(Long dealerUserId, String status) {
        LambdaQueryWrapper<DealerOrder> wrapper = new LambdaQueryWrapper<DealerOrder>()
                .eq(DealerOrder::getDealerUserId, dealerUserId)
                .orderByDesc(DealerOrder::getId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DealerOrder::getStatus, status);
        }
        return list(wrapper);
    }

    @Override
    public List<DealerOrder> listByShop(String status) {
        LambdaQueryWrapper<DealerOrder> wrapper = new LambdaQueryWrapper<DealerOrder>()
                .orderByDesc(DealerOrder::getId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DealerOrder::getStatus, status);
        }
        return list(wrapper);
    }
}
