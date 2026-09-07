package com.shopplatform.domain.offlinestore.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.offlinestore.entity.VerifyLog;
import com.shopplatform.domain.offlinestore.service.PickupVerifyService;
import com.shopplatform.domain.offlinestore.service.VerifyLogService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.shop.entity.StoreRole;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.shop.service.StoreRoleService;
import com.shopplatform.domain.shop.service.StoreUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class PickupVerifyServiceImpl implements PickupVerifyService {

    private final OrderService orderService;
    private final StoreUserService storeUserService;
    private final StoreRoleService storeRoleService;
    private final VerifyLogService verifyLogService;

    public PickupVerifyServiceImpl(OrderService orderService,
                                    StoreUserService storeUserService,
                                    StoreRoleService storeRoleService,
                                    VerifyLogService verifyLogService) {
        this.orderService = orderService;
        this.storeUserService = storeUserService;
        this.storeRoleService = storeRoleService;
        this.verifyLogService = verifyLogService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order verify(String verifyCode, Long operatorUserId) {
        if (!StringUtils.hasText(verifyCode)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请输入核销码");
        }
        // 按 shop_id + pickup_code 查——租户过滤由 MyBatis-Plus 拦截器自动拼接，无需再手动限定 shopId。
        Order order = orderService.getOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getDeliveryType, "pickup")
                .eq(Order::getPickupCode, verifyCode.trim()));
        if (order == null) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_INVALID, "核销码无效");
        }
        if (!"paid".equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单尚未支付，不能核销");
        }

        StoreUser operator = storeUserService.getByIdWithTenant(operatorUserId);
        StoreRole role = operator.getRoleId() == null ? null : storeRoleService.getByIdWithTenant(operator.getRoleId());
        // 数据权限：dataScope=store 的门店店员只能核销自己所属门店的订单，其余角色（含店主）不受限。
        if (role != null && "store".equals(role.getDataScope())) {
            if (operator.getStoreOfflineId() == null || !operator.getStoreOfflineId().equals(order.getPickupStoreId())) {
                throw new TenantAccessDeniedException("该订单不属于当前店员所在门店");
            }
        }

        // 防重复核销的关键条件更新：只有 delivery_status 仍是 pending 时才允许流转，
        // 与 OrderServiceImpl#confirmReceipt 同样的写法——数据库行级更新天然是并发安全的，
        // 两个并发核销请求只有一个能把 0 行更新成 1 行。
        boolean updated = orderService.update(Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, order.getId())
                .eq(Order::getDeliveryStatus, "pending")
                .set(Order::getDeliveryStatus, "received")
                .set(Order::getReceiptStatus, "confirmed")
                .set(Order::getOrderStatus, "finished"));
        if (!updated) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单已核销或状态不允许核销");
        }

        VerifyLog log = new VerifyLog();
        log.setOrderId(order.getId());
        log.setStoreId(order.getPickupStoreId());
        log.setClerkId(operatorUserId);
        log.setVerifyCode(verifyCode.trim());
        log.setVerifyTime(LocalDateTime.now());
        verifyLogService.save(log);

        order.setDeliveryStatus("received");
        order.setReceiptStatus("confirmed");
        order.setOrderStatus("finished");
        return order;
    }
}
