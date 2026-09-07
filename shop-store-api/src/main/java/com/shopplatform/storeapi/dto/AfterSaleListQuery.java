package com.shopplatform.storeapi.dto;

/**
 * 商户端售后单列表查询条件。对应原型 store/after-sale-list.html 的筛选栏与Tab（审核中/已同意/退款中/已完成）。
 */
public record AfterSaleListQuery(
        String status,
        Integer pageNum,
        Integer pageSize
) {
    public int pageNumOrDefault() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public int pageSizeOrDefault() {
        return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
    }
}
