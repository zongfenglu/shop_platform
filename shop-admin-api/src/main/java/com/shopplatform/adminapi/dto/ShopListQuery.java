package com.shopplatform.adminapi.dto;

/**
 * 商城列表查询条件。对应原型 admin/shop-list.html 的筛选栏。
 */
public record ShopListQuery(
        String keyword,
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
