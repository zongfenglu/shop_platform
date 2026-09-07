package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.dealer.entity.DealerWithdraw;
import com.shopplatform.domain.dealer.service.DealerWithdrawService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 提现审核。对应原型 store/distribution.html 提现审核 Tab。 */
@RestController
@RequestMapping("/store/dealer/withdraws")
public class StoreDealerWithdrawController {

    private final DealerWithdrawService dealerWithdrawService;

    public StoreDealerWithdrawController(DealerWithdrawService dealerWithdrawService) {
        this.dealerWithdrawService = dealerWithdrawService;
    }

    @GetMapping
    public Result<List<DealerWithdraw>> list(@RequestParam(required = false) String status) {
        return Result.ok(dealerWithdrawService.listByShop(status));
    }

    @PostMapping("/{id}/approve")
    public Result<DealerWithdraw> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(dealerWithdrawService.approve(id, body.getOrDefault("remark", "")));
    }

    @PostMapping("/{id}/reject")
    public Result<DealerWithdraw> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(dealerWithdrawService.reject(id, body.getOrDefault("remark", "")));
    }
}
