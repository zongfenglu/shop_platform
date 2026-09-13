package com.shopplatform.domain.order.service;

import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.pricing.PriceContext;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

/**
 * 下单服务。见文档二 §4.2 下单支付流程、文档三 §5 库存与并发（下单减库存）。
 */
public interface OrderService extends TenantSafeService<Order> {

    /**
     * 创建订单：调用价格计算引擎算价 → 乐观锁扣减各SKU库存（任一SKU库存不足则整单失败并回滚已扣减的库存）
     * → 落地 order + order_goods + order_address。
     * <p>
     * 全程只有这一个入口产生订单，不允许在别处手写"新建订单"的逻辑——
     * 保证价格计算永远走同一套引擎（文档三 §4 的核心要求）。
     */
    Order createOrder(CreateOrderCommand command);

    /** 按订单号查询，支付回调/主动查单场景专用——这两个场景只有商户侧的 out_trade_no，没有雪花主键。 */
    Order findByOrderNo(String orderNo);

    /** 记录未支付订单本次选择的渠道，供回调丢失时主动查单使用。 */
    boolean recordPayMethod(String orderNo, String payMethod);

    /**
     * 支付成功状态流转：{@code pay_status='unpaid' -> 'paid'}，用 WHERE pay_status='unpaid' 做乐观锁条件，
     * 防止同一笔支付被并发处理两次（正常情况下 {@link com.shopplatform.domain.pay.service.PayNotifyLogService}
     * 的幂等表已经拦掉了重复回调，这里的条件更新是双重保险，同时也防住"超时关单"与"支付成功回调"的竞态）。
     *
     * @return true 表示状态流转成功；false 表示订单已经是 paid（或其他非 unpaid 状态），调用方应视为已处理，不再重复扣减/加分等副作用
     */
    boolean markPaid(String orderNo, String transactionId, String payMethod);

    /**
     * 发货：首次把 {@code delivery_status} 从 pending 打到 shipped，并落一条 {@code order_package}。
     * {@code orderGoodsIds} 为空表示发出当前尚未发出的全部行；非空则只发指定行。
     * 同一行不能重复发；全部发出后再次调用会失败。已 shipped 的订单可以继续补包裹。
     */
    void ship(Long orderId, ShipCommand command);

    /** 确认收货：{@code delivery_status='shipped' -> 'received'}，{@code receipt_status -> 'confirmed'}，订单进入 finished。 */
    void confirmReceipt(Long orderId);

    /**
     * 取消订单：仅允许 {@code pay_status='unpaid'} 的订单被取消（已支付订单走售后流程，不走这里），
     * 状态流转成功后回补已扣减的 SKU 库存。用于用户主动取消，也用于超时自动关单（见 RocketMQ 延时消息消费者）。
     *
     * @return true 表示成功取消；false 表示订单已经不是 unpaid 状态（比如支付回调先一步到达），调用方不应重复回补库存
     */
    boolean cancel(Long orderId, String reason);

    /**
     * 系统发起的退款关单（拼团超时未成团用）：仅处理已支付订单，状态流转 {@code pay_status='paid' -> 'refunded'}、
     * {@code order_status -> 'cancelled'}，回补库存并退回优惠券。未支付订单应改用 {@link #cancel}。
     * 与 {@link #cancel} 一样，越权校验单独先行判定。真实资金退款由支付域在 M5 接入，此处先做状态与库存。
     *
     * @return true 表示成功退款关单；false 表示订单不是 paid 状态（无需处理）
     */
    boolean systemRefund(Long orderId, String reason);

    /** 列出某拼团记录下的全部订单（GroupExpireJob 关单/退款用），按 id 升序 */
    List<Order> listByGroupRecordId(Long groupRecordId);

    record ShipCommand(
            String expressCompany,
            String expressNo,
            List<Long> orderGoodsIds
    ) {
    }

    record CreateOrderCommand(
            Long userId,
            List<PriceContext.PriceItem> items,
            String deliveryType,
            /** deliveryType=pickup 时必填：消费者选择的自提门店 id（offline_store.id） */
            Long pickupStoreId,
            Long freightTemplateId,
            Long couponId,
            Integer pointsToUse,
            String activityType,
            Long activityId,
            /** 拼团订单关联的 group_record.id（参团/开团时由 CheckoutAppService 传入）；非拼团订单传 null */
            Long groupRecordId,
            String orderSource,
            String buyerRemark,
            AddressInfo address
    ) {
    }

    record AddressInfo(
            String name,
            String phone,
            String province,
            String city,
            String region,
            String detail
    ) {
    }
}
