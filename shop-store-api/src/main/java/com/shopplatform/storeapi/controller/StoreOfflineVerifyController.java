package com.shopplatform.storeapi.controller;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.offlinestore.entity.OfflineStore;
import com.shopplatform.domain.offlinestore.entity.VerifyLog;
import com.shopplatform.domain.offlinestore.service.OfflineStoreService;
import com.shopplatform.domain.offlinestore.service.PickupVerifyService;
import com.shopplatform.domain.offlinestore.service.VerifyLogService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.shop.entity.StoreRole;
import com.shopplatform.domain.shop.entity.StoreUser;
import com.shopplatform.domain.shop.service.StoreRoleService;
import com.shopplatform.domain.shop.service.StoreUserService;
import com.shopplatform.framework.security.LoginUserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 门店自提核销，对应 docs/prototype/store/store-offline.html 的核销记录页。
 * 数据权限：dataScope=store 的门店店员只能核销/查看自己所属门店，见 {@link PickupVerifyService}。
 */
@RestController
@RequestMapping("/store/offline")
public class StoreOfflineVerifyController {

    private final PickupVerifyService pickupVerifyService;
    private final VerifyLogService verifyLogService;
    private final StoreUserService storeUserService;
    private final StoreRoleService storeRoleService;
    private final OfflineStoreService offlineStoreService;

    public StoreOfflineVerifyController(PickupVerifyService pickupVerifyService,
                                         VerifyLogService verifyLogService,
                                         StoreUserService storeUserService,
                                         StoreRoleService storeRoleService,
                                         OfflineStoreService offlineStoreService) {
        this.pickupVerifyService = pickupVerifyService;
        this.verifyLogService = verifyLogService;
        this.storeUserService = storeUserService;
        this.storeRoleService = storeRoleService;
        this.offlineStoreService = offlineStoreService;
    }

    @PostMapping("/verify")
    public Result<Order> verify(@Valid @RequestBody VerifyRequest request) {
        Order order = pickupVerifyService.verify(request.verifyCode(), requireLoginUserId());
        return Result.ok(order);
    }

    @GetMapping("/verify-logs")
    public Result<List<VerifyLogItem>> verifyLogs() {
        Long effectiveStoreId = effectiveStoreIdForCurrentOperator();
        List<VerifyLog> logs = verifyLogService.listByStore(effectiveStoreId);

        Map<Long, String> storeNames = offlineStoreService.list().stream()
                .collect(Collectors.toMap(OfflineStore::getId, OfflineStore::getName, (left, right) -> left));
        Map<Long, String> clerkNames = storeUserService.list().stream()
                .collect(Collectors.toMap(StoreUser::getId,
                        user -> user.getRealName() == null ? user.getUsername() : user.getRealName(),
                        (left, right) -> left));

        List<VerifyLogItem> items = logs.stream()
                .map(log -> new VerifyLogItem(log.getId(), log.getOrderId(), log.getStoreId(),
                        storeNames.get(log.getStoreId()), log.getClerkId(), clerkNames.get(log.getClerkId()),
                        log.getVerifyCode(), log.getVerifyTime()))
                .toList();
        return Result.ok(items);
    }

    /** dataScope=store 的店员强制限定为自己所属门店；其余角色（含店主）看全部。 */
    private Long effectiveStoreIdForCurrentOperator() {
        StoreUser operator = storeUserService.getByIdWithTenant(requireLoginUserId());
        StoreRole role = operator.getRoleId() == null ? null : storeRoleService.getByIdWithTenant(operator.getRoleId());
        if (role != null && "store".equals(role.getDataScope())) {
            return operator.getStoreOfflineId();
        }
        return null;
    }

    private Long requireLoginUserId() {
        LoginUserContext.LoginUser operator = LoginUserContext.get();
        if (operator == null || operator.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录商户后台");
        }
        return operator.userId();
    }

    public record VerifyRequest(@NotBlank String verifyCode) {
    }

    public record VerifyLogItem(Long id, Long orderId, Long storeId, String storeName, Long clerkId,
                                 String clerkName, String verifyCode, LocalDateTime verifyTime) {
    }
}
