package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.platform.service.SysLogService;
import com.shopplatform.domain.shop.entity.ShopInvoice;
import com.shopplatform.domain.shop.entity.ShopOrder;
import com.shopplatform.domain.shop.service.ShopInvoiceService;
import com.shopplatform.domain.shop.service.ShopSubscriptionService;
import com.shopplatform.framework.tenant.TenantContext;
import com.shopplatform.framework.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 我的套餐。对应原型 store/my-package.html。
 * 续费/升级生成待确认订购单（线下转账），由超管确认到账后延长有效期。
 */
@RestController
@RequestMapping("/store/package")
public class StorePackageController {

    private final ShopSubscriptionService shopSubscriptionService;
    private final ShopInvoiceService shopInvoiceService;
    private final SysLogService sysLogService;

    public StorePackageController(ShopSubscriptionService shopSubscriptionService,
                                  ShopInvoiceService shopInvoiceService,
                                  SysLogService sysLogService) {
        this.shopSubscriptionService = shopSubscriptionService;
        this.shopInvoiceService = shopInvoiceService;
        this.sysLogService = sysLogService;
    }

    @GetMapping
    public Result<ShopSubscriptionService.Overview> overview() {
        return Result.ok(shopSubscriptionService.overview(TenantContext.getRequired()));
    }

    @PostMapping("/orders")
    public Result<ShopOrder> placeOrder(@Valid @RequestBody PlaceRequest request, HttpServletRequest httpRequest) {
        int months = request.durationMonth() == null ? 12 : request.durationMonth();
        ShopOrder order = shopSubscriptionService.placeOrder(
                TenantContext.getRequired(), request.type(), request.packageTplId(), months);
        sysLogService.record(order.getShopId(), "package-order",
                "提交套餐订购 " + order.getType() + " " + months + "个月", ClientIp.resolve(httpRequest));
        return Result.ok(order);
    }

    @GetMapping("/invoices")
    public Result<List<ShopInvoice>> invoices() {
        return Result.ok(shopInvoiceService.listByShop(TenantContext.getRequired()));
    }

    @PostMapping("/invoices")
    public Result<ShopInvoice> applyInvoice(@Valid @RequestBody InvoiceRequest request, HttpServletRequest httpRequest) {
        ShopInvoice row = shopInvoiceService.apply(
                TenantContext.getRequired(), request.shopOrderId(), request.title(), request.taxNo());
        sysLogService.record(row.getShopId(), "invoice-apply",
                "申请发票 " + row.getTitle(), ClientIp.resolve(httpRequest));
        return Result.ok(row);
    }

    public record PlaceRequest(
            @NotBlank String type,
            Long packageTplId,
            Integer durationMonth
    ) {
    }

    public record InvoiceRequest(
            @NotNull Long shopOrderId,
            @NotBlank String title,
            @NotBlank String taxNo
    ) {
    }
}
