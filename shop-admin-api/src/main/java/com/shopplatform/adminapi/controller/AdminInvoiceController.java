package com.shopplatform.adminapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shopplatform.adminapi.dto.ShopInvoiceItem;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.platform.service.SysLogService;
import com.shopplatform.domain.shop.entity.Shop;
import com.shopplatform.domain.shop.entity.ShopInvoice;
import com.shopplatform.domain.shop.entity.ShopOrder;
import com.shopplatform.domain.shop.service.ShopInvoiceService;
import com.shopplatform.domain.shop.service.ShopOrderService;
import com.shopplatform.domain.shop.service.ShopService;
import com.shopplatform.framework.web.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/invoices")
public class AdminInvoiceController {

    private final ShopInvoiceService shopInvoiceService;
    private final ShopService shopService;
    private final ShopOrderService shopOrderService;
    private final SysLogService sysLogService;

    public AdminInvoiceController(ShopInvoiceService shopInvoiceService,
                                  ShopService shopService,
                                  ShopOrderService shopOrderService,
                                  SysLogService sysLogService) {
        this.shopInvoiceService = shopInvoiceService;
        this.shopService = shopService;
        this.shopOrderService = shopOrderService;
        this.sysLogService = sysLogService;
    }

    @GetMapping
    public Result<List<ShopInvoiceItem>> list(@RequestParam(required = false) String status) {
        var wrapper = Wrappers.<ShopInvoice>lambdaQuery().orderByDesc(ShopInvoice::getCreateTime);
        if (StringUtils.hasText(status)) {
            wrapper.eq(ShopInvoice::getStatus, status);
        }
        return Result.ok(toItems(shopInvoiceService.list(wrapper)));
    }

    @PostMapping("/{id}/issue")
    public Result<ShopInvoiceItem> issue(@PathVariable Long id,
                                         @RequestBody(required = false) IssueRequest body,
                                         HttpServletRequest request) {
        ShopInvoice row = shopInvoiceService.issue(id, body == null ? null : body.invoiceNo());
        sysLogService.record(row.getShopId(), "invoice-issue", "开票 " + row.getInvoiceNo(), ClientIp.resolve(request));
        return Result.ok(toItems(List.of(row)).get(0));
    }

    @PostMapping("/{id}/reject")
    public Result<ShopInvoiceItem> reject(@PathVariable Long id,
                                          @RequestBody(required = false) RejectRequest body,
                                          HttpServletRequest request) {
        ShopInvoice row = shopInvoiceService.reject(id, body == null ? null : body.reason());
        sysLogService.record(row.getShopId(), "invoice-reject", "驳回发票申请", ClientIp.resolve(request));
        return Result.ok(toItems(List.of(row)).get(0));
    }

    private List<ShopInvoiceItem> toItems(List<ShopInvoice> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, Shop> shops = shopService.listByIds(rows.stream().map(ShopInvoice::getShopId).distinct().toList())
                .stream().collect(Collectors.toMap(Shop::getId, s -> s, (a, b) -> a));
        List<Long> orderIds = rows.stream().map(ShopInvoice::getShopOrderId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ShopOrder> orders = orderIds.isEmpty() ? Map.of() : shopOrderService.listByIds(orderIds).stream()
                .collect(Collectors.toMap(ShopOrder::getId, o -> o, (a, b) -> a));
        return rows.stream().map(row -> {
            Shop shop = shops.get(row.getShopId());
            ShopOrder order = orders.get(row.getShopOrderId());
            return new ShopInvoiceItem(
                    row.getId(),
                    row.getShopId(),
                    shop == null ? ("#" + row.getShopId()) : shop.getName(),
                    row.getShopOrderId(),
                    order == null ? null : order.getOrderNo(),
                    row.getTitle(),
                    row.getTaxNo(),
                    row.getAmount(),
                    row.getStatus(),
                    row.getInvoiceNo(),
                    row.getIssueTime(),
                    row.getRejectReason(),
                    row.getCreateTime()
            );
        }).toList();
    }

    public record IssueRequest(String invoiceNo) {
    }

    public record RejectRequest(String reason) {
    }
}
