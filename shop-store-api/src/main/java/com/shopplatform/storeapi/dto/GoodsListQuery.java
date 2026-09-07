package com.shopplatform.storeapi.dto;

/**
 * 商品列表查询条件。对应原型 store/goods-list.html 的筛选栏与Tab（出售中/仓库中/已售罄/回收站）。
 */
public record GoodsListQuery(
        String keyword,
        String status,
        Long categoryId,
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
