package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.dealer.entity.DealerUser;
import com.shopplatform.domain.dealer.service.DealerUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 分销商管理。对应原型 store/distribution.html 分销商列表/审核。 */
@RestController
@RequestMapping("/store/dealer/users")
public class StoreDealerUserController {

    private final DealerUserService dealerUserService;

    public StoreDealerUserController(DealerUserService dealerUserService) {
        this.dealerUserService = dealerUserService;
    }

    @GetMapping
    public Result<List<DealerUser>> list(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String keyword) {
        return Result.ok(dealerUserService.listByShop(status, keyword));
    }

    @PostMapping("/{id}/approve")
    public Result<DealerUser> approve(@PathVariable Long id) {
        return Result.ok(dealerUserService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public Result<DealerUser> reject(@PathVariable Long id) {
        return Result.ok(dealerUserService.reject(id));
    }

    @PostMapping("/{id}/disable")
    public Result<DealerUser> disable(@PathVariable Long id) {
        return Result.ok(dealerUserService.disable(id));
    }
}
