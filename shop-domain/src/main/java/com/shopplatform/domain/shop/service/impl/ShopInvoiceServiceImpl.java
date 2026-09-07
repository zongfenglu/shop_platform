package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.shop.entity.ShopInvoice;
import com.shopplatform.domain.shop.entity.ShopOrder;
import com.shopplatform.domain.shop.mapper.ShopInvoiceMapper;
import com.shopplatform.domain.shop.service.ShopInvoiceService;
import com.shopplatform.domain.shop.service.ShopOrderService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ShopInvoiceServiceImpl extends ServiceImpl<ShopInvoiceMapper, ShopInvoice>
        implements ShopInvoiceService {

    private final ShopOrderService shopOrderService;

    public ShopInvoiceServiceImpl(ShopOrderService shopOrderService) {
        this.shopOrderService = shopOrderService;
    }

    @Override
    public ShopInvoice apply(Long shopId, Long shopOrderId, String title, String taxNo) {
        if (shopOrderId == null || !StringUtils.hasText(title) || !StringUtils.hasText(taxNo)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "订购单、发票抬头和税号不能为空");
        }
        ShopOrder order = shopOrderService.getOne(
                Wrappers.<ShopOrder>lambdaQuery().eq(ShopOrder::getId, shopOrderId));
        if (order == null || !shopId.equals(order.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订购单不存在");
        }
        if (!"paid".equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.INVOICE_ORDER_NOT_PAID);
        }
        ShopInvoice existing = this.getOne(Wrappers.<ShopInvoice>lambdaQuery()
                .eq(ShopInvoice::getShopOrderId, shopOrderId)
                .orderByDesc(ShopInvoice::getCreateTime)
                .last("LIMIT 1"));
        if (existing != null && ("applying".equals(existing.getStatus()) || "issued".equals(existing.getStatus()))) {
            throw new BusinessException(ErrorCode.INVOICE_ALREADY_EXISTS);
        }
        if (existing != null && "rejected".equals(existing.getStatus())) {
            existing.setTitle(title.trim());
            existing.setTaxNo(taxNo.trim());
            existing.setAmount(order.getAmount());
            existing.setStatus("applying");
            existing.setRejectReason(null);
            existing.setInvoiceNo(null);
            existing.setIssueTime(null);
            this.updateById(existing);
            return existing;
        }
        ShopInvoice row = new ShopInvoice();
        row.setShopId(shopId);
        row.setShopOrderId(shopOrderId);
        row.setTitle(title.trim());
        row.setTaxNo(taxNo.trim());
        row.setAmount(order.getAmount());
        row.setStatus("applying");
        this.save(row);
        return row;
    }

    @Override
    public ShopInvoice issue(Long id, String invoiceNo) {
        ShopInvoice row = require(id);
        if (!"applying".equals(row.getStatus())) {
            throw new BusinessException(ErrorCode.INVOICE_STATUS_INVALID);
        }
        String no = StringUtils.hasText(invoiceNo)
                ? invoiceNo.trim()
                : "INV" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + row.getId();
        row.setInvoiceNo(no);
        row.setStatus("issued");
        row.setIssueTime(LocalDateTime.now());
        row.setRejectReason(null);
        this.updateById(row);
        return row;
    }

    @Override
    public ShopInvoice reject(Long id, String reason) {
        ShopInvoice row = require(id);
        if (!"applying".equals(row.getStatus())) {
            throw new BusinessException(ErrorCode.INVOICE_STATUS_INVALID);
        }
        row.setStatus("rejected");
        row.setRejectReason(StringUtils.hasText(reason) ? reason.trim() : "不符合开票要求");
        this.updateById(row);
        return row;
    }

    @Override
    public List<ShopInvoice> listByShop(Long shopId) {
        return this.list(Wrappers.<ShopInvoice>lambdaQuery()
                .eq(ShopInvoice::getShopId, shopId)
                .orderByDesc(ShopInvoice::getCreateTime));
    }

    private ShopInvoice require(Long id) {
        ShopInvoice row = this.getOne(Wrappers.<ShopInvoice>lambdaQuery().eq(ShopInvoice::getId, id));
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "发票不存在");
        }
        return row;
    }
}
