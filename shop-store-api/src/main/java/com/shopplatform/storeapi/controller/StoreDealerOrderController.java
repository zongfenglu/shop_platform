package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.dealer.entity.DealerOrder;
import com.shopplatform.domain.dealer.service.DealerOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 分销订单/佣金管理。对应原型 store/distribution.html 分销订单。 */
@RestController
@RequestMapping("/store/dealer/orders")
public class StoreDealerOrderController {

    private final DealerOrderService dealerOrderService;

    public StoreDealerOrderController(DealerOrderService dealerOrderService) {
        this.dealerOrderService = dealerOrderService;
    }

    @GetMapping
    public Result<List<DealerOrder>> list(@RequestParam(required = false) String status) {
        return Result.ok(dealerOrderService.listByShop(status));
    }
}
