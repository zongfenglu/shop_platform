package com.shopplatform.adminapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShopInvoiceItem(
        Long id,
        Long shopId,
        String shopName,
        Long shopOrderId,
        String orderNo,
        String title,
        String taxNo,
        BigDecimal amount,
        String status,
        String invoiceNo,
        LocalDateTime issueTime,
        String rejectReason,
        LocalDateTime createTime
) {
}
