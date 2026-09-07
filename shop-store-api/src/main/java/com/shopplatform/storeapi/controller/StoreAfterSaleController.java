package com.shopplatform.storeapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.storeapi.dto.AfterSaleListQuery;
import com.shopplatform.storeapi.dto.AuditAfterSaleRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 商户端售后管理。对应原型 store/after-sale-list.html。
 * 状态机：applying(审核中) -[同意/拒绝]-> approved/rejected；
 * approved -[退货退款需先确认收货]-> return_shipped -[执行退款]-> refunding -> refunded。
 */
@RestController
@RequestMapping("/store/after-sale")
public class StoreAfterSaleController {

    private final AfterSaleService afterSaleService;

    public StoreAfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @GetMapping
    public Result<IPage<AfterSale>> list(AfterSaleListQuery query) {
        var wrapper = Wrappers.<AfterSale>lambdaQuery();
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(AfterSale::getStatus, query.status());
        }
        wrapper.orderByDesc(AfterSale::getCreateTime);
        Page<AfterSale> page = new Page<>(query.pageNumOrDefault(), query.pageSizeOrDefault());
        return Result.ok(afterSaleService.page(page, wrapper));
    }

    @GetMapping("/{id}")
    public Result<AfterSale> detail(@PathVariable Long id) {
        return Result.ok(afterSaleService.getByIdWithTenant(id));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) AuditAfterSaleRequest request) {
        afterSaleService.approve(id, request == null ? null : request.auditRemark());
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) AuditAfterSaleRequest request) {
        afterSaleService.reject(id, request == null ? null : request.auditRemark());
        return Result.ok();
    }

    /** 仅退款场景商户审核同意后即可直接执行退款；退货退款场景需买家先 confirm-return-shipped。 */
    @PostMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Long id) {
        afterSaleService.executeRefund(id);
        return Result.ok();
    }
}
