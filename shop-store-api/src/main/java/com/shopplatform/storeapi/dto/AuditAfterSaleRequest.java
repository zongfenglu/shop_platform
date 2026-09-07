package com.shopplatform.storeapi.dto;

/**
 * 商户端售后审核请求。对应原型 store/after-sale-list.html 的审核弹窗。
 */
public record AuditAfterSaleRequest(
        String auditRemark
) {
}
