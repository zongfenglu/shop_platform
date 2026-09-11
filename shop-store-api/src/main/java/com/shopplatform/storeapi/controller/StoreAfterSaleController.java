package com.shopplatform.storeapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.aftersale.entity.AfterSale;
import com.shopplatform.domain.aftersale.service.AfterSaleService;
import com.shopplatform.domain.setting.entity.ReturnAddress;
import com.shopplatform.domain.setting.service.ReturnAddressService;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.storeapi.dto.AfterSaleListQuery;
import com.shopplatform.storeapi.dto.AuditAfterSaleRequest;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
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
    private final ReturnAddressService returnAddressService;
    private final ObjectMapper objectMapper;

    public StoreAfterSaleController(AfterSaleService afterSaleService,
                                    ReturnAddressService returnAddressService,
                                    ObjectMapper objectMapper) {
        this.afterSaleService = afterSaleService;
        this.returnAddressService = returnAddressService;
        this.objectMapper = objectMapper;
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
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) AuditAfterSaleRequest request) {
        AfterSale afterSale = afterSaleService.getByIdWithTenant(id);
        if ("return_refund".equals(afterSale.getType())) {
            Long addressId = request == null ? null : request.returnAddressId();
            ReturnAddress address = addressId == null
                    ? returnAddressService.listAll().stream()
                        .filter(a -> Boolean.TRUE.equals(a.getIsDefault()) && "enabled".equals(a.getStatus()))
                        .findFirst().orElse(null)
                    : returnAddressService.getByIdWithTenant(addressId);
            if (address == null || !"enabled".equals(address.getStatus())) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "同意退货前请先选择启用的退货地址");
            }
            afterSale.setReturnAddressSnapshot(toAddressSnapshot(address));
            afterSaleService.updateById(afterSale);
        }
        afterSaleService.approve(id, request == null ? null : request.auditRemark());
        return Result.ok();
    }

    private String toAddressSnapshot(ReturnAddress address) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "contactName", address.getContactName(), "phone", address.getPhone(),
                    "province", address.getProvince(), "city", address.getCity(),
                    "district", address.getDistrict(), "detail", address.getDetail(),
                    "postalCode", address.getPostalCode() == null ? "" : address.getPostalCode()));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退货地址快照保存失败");
        }
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
