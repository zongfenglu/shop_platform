package com.shopplatform.clientapi.controller;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.dealer.entity.DealerOrder;
import com.shopplatform.domain.dealer.entity.DealerSetting;
import com.shopplatform.domain.dealer.entity.DealerUser;
import com.shopplatform.domain.dealer.entity.DealerWithdraw;
import com.shopplatform.domain.dealer.service.DealerOrderService;
import com.shopplatform.domain.dealer.service.DealerSettingService;
import com.shopplatform.domain.dealer.service.DealerUserService;
import com.shopplatform.domain.dealer.service.DealerWithdrawService;
import com.shopplatform.framework.security.LoginUserContext;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 消费者分销中心。对应原型 h5/distribution-center.html。 */
@RestController
@RequestMapping("/api/dealer")
public class ConsumerDealerController {

    private final DealerSettingService dealerSettingService;
    private final DealerUserService dealerUserService;
    private final DealerOrderService dealerOrderService;
    private final DealerWithdrawService dealerWithdrawService;

    public ConsumerDealerController(DealerSettingService dealerSettingService,
                                     DealerUserService dealerUserService,
                                     DealerOrderService dealerOrderService,
                                     DealerWithdrawService dealerWithdrawService) {
        this.dealerSettingService = dealerSettingService;
        this.dealerUserService = dealerUserService;
        this.dealerOrderService = dealerOrderService;
        this.dealerWithdrawService = dealerWithdrawService;
    }

    @GetMapping("/setting")
    public Result<Map<String, Object>> setting() {
        DealerSetting setting = dealerSettingService.getOrCreate();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("isEnable", setting.getIsEnable() == 1);
        map.put("commissionRate", setting.getCommissionRate());
        map.put("minWithdraw", setting.getMinWithdraw());
        return Result.ok(map);
    }

    @PostMapping("/apply")
    public Result<DealerUser> apply() {
        return Result.ok(dealerUserService.apply(requireLoginUserId()));
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        Long userId = requireLoginUserId();
        DealerUser du = dealerUserService.getMyDealer(userId);
        if (du == null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", "none");
            return Result.ok(m);
        }
        List<DealerUser> team = dealerUserService.listMyTeam(userId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", du.getId().toString());
        map.put("status", du.getStatus());
        map.put("totalCommission", du.getTotalCommission());
        map.put("availableCommission", du.getAvailableCommission());
        map.put("frozenCommission", du.getFrozenCommission());
        map.put("teamCount", team.size());
        return Result.ok(map);
    }

    @GetMapping("/orders")
    public Result<List<DealerOrder>> orders(@RequestParam(required = false) String status) {
        Long userId = requireLoginUserId();
        DealerUser du = dealerUserService.getMyDealer(userId);
        if (du == null) return Result.ok(List.of());
        return Result.ok(dealerOrderService.listByDealer(du.getId(), status));
    }

    @GetMapping("/team")
    public Result<List<DealerUser>> team() {
        return Result.ok(dealerUserService.listMyTeam(requireLoginUserId()));
    }

    // ---- 提现 ----

    @PostMapping("/withdraw")
    public Result<DealerWithdraw> withdraw(@RequestBody Map<String, Object> body) {
        java.math.BigDecimal amount = new java.math.BigDecimal(body.get("amount").toString());
        String method = (String) body.getOrDefault("method", "wechat");
        String accountInfo = (String) body.getOrDefault("accountInfo", "");
        return Result.ok(dealerWithdrawService.apply(requireLoginUserId(), amount, method, accountInfo));
    }

    @GetMapping("/withdraws")
    public Result<List<DealerWithdraw>> withdraws() {
        Long userId = requireLoginUserId();
        DealerUser du = dealerUserService.getMyDealer(userId);
        if (du == null) return Result.ok(List.of());
        return Result.ok(dealerWithdrawService.listByDealer(du.getId()));
    }

    private Long requireLoginUserId() {
        LoginUserContext.LoginUser user = LoginUserContext.get();
        if (user == null || user.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return user.userId();
    }
}
