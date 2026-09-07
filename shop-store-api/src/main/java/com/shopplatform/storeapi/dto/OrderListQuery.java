package com.shopplatform.storeapi.dto;

/**
 * 商户订单列表查询条件。对应原型 store/order-list.html 的筛选栏与Tab（待付款/待发货/待收货/已完成/已取消）。
 */
public record OrderListQuery(
        String orderNo,
        String payStatus,
        String deliveryStatus,
        String orderStatus,
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
