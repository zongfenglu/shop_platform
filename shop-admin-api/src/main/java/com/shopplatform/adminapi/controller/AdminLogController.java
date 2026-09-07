package com.shopplatform.adminapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shopplatform.adminapi.dto.SysLogItem;
import com.shopplatform.adminapi.dto.SysLogListQuery;
import com.shopplatform.adminapi.dto.SysLogPageResponse;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.platform.entity.SysLog;
import com.shopplatform.domain.platform.service.SysLogService;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.service.ShopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/logs")
public class AdminLogController {

    private final SysLogService sysLogService;
    private final ShopService shopService;

    public AdminLogController(SysLogService sysLogService, ShopService shopService) {
        this.sysLogService = sysLogService;
        this.shopService = shopService;
    }

    @GetMapping
    public Result<SysLogPageResponse> page(SysLogListQuery query) {
        IPage<SysLog> page = sysLogService.page(query.kind(), query.pageNumOrDefault(), query.pageSizeOrDefault());
        List<SysLog> records = page.getRecords();
        Map<Long, Shop> shops = Map.of();
        List<Long> shopIds = records.stream().map(SysLog::getShopId).filter(Objects::nonNull).distinct().toList();
        if (!shopIds.isEmpty()) {
            shops = shopService.listByIds(shopIds).stream().collect(Collectors.toMap(Shop::getId, s -> s));
        }
        Map<Long, Shop> shopMap = shops;
        List<SysLogItem> items = records.stream().map(row -> {
            Shop shop = row.getShopId() == null ? null : shopMap.get(row.getShopId());
            return new SysLogItem(
                    row.getId(),
                    row.getShopId(),
                    shop == null ? null : shop.getName(),
                    row.getOperatorType(),
                    row.getOperatorName(),
                    row.getByPlatform(),
                    row.getAction(),
                    row.getDescription(),
                    row.getIp(),
                    row.getCreateTime()
            );
        }).toList();
        return Result.ok(new SysLogPageResponse(items, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages()));
    }
}
