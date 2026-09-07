package com.shopplatform.clientapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.offlinestore.entity.OfflineStore;
import com.shopplatform.domain.offlinestore.service.OfflineStoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 消费者端可选自提门店列表，对应原型 h5/store-locator.html。无需登录——与商品浏览一样公开。 */
@RestController
@RequestMapping("/api/offline-stores")
public class ConsumerOfflineStoreController {

    private final OfflineStoreService offlineStoreService;

    public ConsumerOfflineStoreController(OfflineStoreService offlineStoreService) {
        this.offlineStoreService = offlineStoreService;
    }

    @GetMapping
    public Result<List<OfflineStore>> list() {
        return Result.ok(offlineStoreService.listEnabled());
    }
}
