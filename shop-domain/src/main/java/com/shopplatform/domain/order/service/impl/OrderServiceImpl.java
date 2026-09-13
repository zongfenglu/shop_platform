package com.shopplatform.domain.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.marketing.service.UserCouponService;
import com.shopplatform.domain.offlinestore.entity.OfflineStore;
import com.shopplatform.domain.offlinestore.service.OfflineStoreService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.entity.OrderAddress;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.entity.OrderPackage;
import com.shopplatform.domain.order.mapper.OrderMapper;
import com.shopplatform.domain.order.mq.OrderCloseDelayProducer;
import com.shopplatform.domain.order.service.OrderAddressService;
import com.shopplatform.domain.order.service.OrderGoodsService;
import com.shopplatform.domain.order.service.OrderPackageService;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.pricing.OrderPriceResult;
import com.shopplatform.domain.pricing.PriceCalculator;
import com.shopplatform.domain.pricing.PriceContext;
import com.shopplatform.framework.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PriceCalculator priceCalculator;
    private final GoodsSkuService goodsSkuService;
    private final OrderGoodsService orderGoodsService;
    private final OrderAddressService orderAddressService;
    private final OrderPackageService orderPackageService;
    private final OrderCloseDelayProducer orderCloseDelayProducer;
    private final UserCouponService userCouponService;
    private final OfflineStoreService offlineStoreService;
    private final ObjectMapper objectMapper;

    /** 未付款自动取消分钟数。M0 阶段先用全局默认值，租户可配置的"交易设置"表属于后续里程碑的独立任务。 */
    @Value("${shop.order.pay-timeout-minutes:30}")
    private int payTimeoutMinutes;

    public OrderServiceImpl(PriceCalculator priceCalculator,
                             GoodsSkuService goodsSkuService,
                             OrderGoodsService orderGoodsService,
                             OrderAddressService orderAddressService,
                             OrderPackageService orderPackageService,
                             OrderCloseDelayProducer orderCloseDelayProducer,
                             UserCouponService userCouponService,
                             OfflineStoreService offlineStoreService,
                             ObjectMapper objectMapper) {
        this.priceCalculator = priceCalculator;
        this.goodsSkuService = goodsSkuService;
        this.orderGoodsService = orderGoodsService;
        this.orderAddressService = orderAddressService;
        this.orderPackageService = orderPackageService;
        this.orderCloseDelayProducer = orderCloseDelayProducer;
        this.userCouponService = userCouponService;
        this.offlineStoreService = offlineStoreService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderCommand cmd) {
        if ("express".equals(cmd.deliveryType()) && cmd.address() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "快递配送订单必须填写收货地址");
        }
        if ("pickup".equals(cmd.deliveryType())) {
            if (cmd.pickupStoreId() == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "请选择自提门店");
            }
            OfflineStore pickupStore = offlineStoreService.getByIdWithTenant(cmd.pickupStoreId());
            if (!"enabled".equals(pickupStore.getStatus())) {
                throw new BusinessException(ErrorCode.OFFLINE_STORE_DISABLED, "该门店暂不支持自提");
            }
        }

        PriceContext priceContext = new PriceContext(
                TenantContext.getRequired(), cmd.userId(), cmd.items(), cmd.deliveryType(),
                cmd.freightTemplateId(), cmd.couponId(), cmd.pointsToUse(), cmd.activityType(), cmd.activityId());
        OrderPriceResult priceResult = priceCalculator.calculate(priceContext);

        // 下单减库存（乐观锁）：逐个SKU扣减，任一失败立即抛异常触发事务回滚，
        // 已扣减成功的SKU库存会随事务回滚一并撤销——不需要手工补偿式回滚。见文档三 §5。
        for (OrderPriceResult.ItemResult item : priceResult.items()) {
            boolean success = goodsSkuService.deductStock(item.skuId(), item.totalNum());
            if (!success) {
                throw new BusinessException(ErrorCode.SKU_STOCK_INSUFFICIENT,
                        "商品「" + item.goodsName() + "」库存不足");
            }
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(cmd.userId());
        order.setSellerId(0L);
        order.setTotalPrice(priceResult.totalPrice());
        order.setDiscountPrice(priceResult.discountPrice());
        order.setCouponPrice(priceResult.couponPrice());
        order.setPointsPrice(priceResult.pointsPrice());
        order.setExpressPrice(priceResult.expressPrice());
        order.setPayPrice(priceResult.payPrice());
        order.setPayStatus("unpaid");
        order.setDeliveryType(cmd.deliveryType());
        if ("pickup".equals(cmd.deliveryType())) {
            order.setPickupStoreId(cmd.pickupStoreId());
            // 6位随机码，同店同一时刻在架的待自提订单量级不会撞码；即便小概率撞上，
            // 核销时也会连带校验 payStatus=paid 与 deliveryStatus=pending，撞码订单核销后立刻从候选集里消失。
            order.setPickupCode(String.format("%06d", ThreadLocalRandom.current().nextInt(1000000)));
        }
        order.setDeliveryStatus("pending");
        order.setReceiptStatus("pending");
        order.setOrderStatus("normal");
        order.setOrderSource(cmd.orderSource() == null ? "mp" : cmd.orderSource());
        order.setBuyerRemark(cmd.buyerRemark());
        order.setCouponId(cmd.couponId());
        order.setPointsNum(cmd.pointsToUse() == null ? 0 : cmd.pointsToUse());
        order.setActivityType(cmd.activityType() == null ? "none" : cmd.activityType());
        order.setActivityId(cmd.activityId());
        order.setGroupRecordId(cmd.groupRecordId());
        this.save(order);

        List<OrderGoods> orderGoodsList = new ArrayList<>();
        for (OrderPriceResult.ItemResult item : priceResult.items()) {
            OrderGoods og = new OrderGoods();
            og.setOrderId(order.getId());
            og.setGoodsId(item.goodsId());
            og.setSkuId(item.skuId());
            og.setGoodsName(item.goodsName());
            og.setImage(item.image());
            og.setSpecText(item.specText());
            og.setGoodsPrice(item.goodsPrice());
            og.setLinePrice(item.linePrice());
            og.setTotalNum(item.totalNum());
            og.setTotalPrice(item.totalPrice());
            og.setDiscountDetail(toJson(item.discountDetail()));
            og.setIsComment(false);
            og.setRefundStatus("none");
            orderGoodsList.add(og);
        }
        orderGoodsService.saveBatch(orderGoodsList);

        // 核销优惠券：价格引擎已校验券可用，这里用原子条件更新 unused→used 兜并发；
        // 失败说明并发下已被核销，抛异常回滚整笔订单（库存、订单、订单商品一并撤销）。
        if (cmd.couponId() != null) {
            if (!userCouponService.tryUse(cmd.couponId(), order.getId())) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE, "优惠券已被使用或已过期");
            }
        }

        if (cmd.address() != null) {
            OrderAddress address = new OrderAddress();
            address.setOrderId(order.getId());
            address.setName(cmd.address().name());
            address.setPhone(cmd.address().phone());
            address.setProvince(cmd.address().province());
            address.setCity(cmd.address().city());
            address.setRegion(cmd.address().region());
            address.setDetail(cmd.address().detail());
            orderAddressService.save(address);
        }

        // 延时消息必须等事务真正提交后再发——否则事务回滚了但消息已经发出去，消费者会对着一笔不存在的订单空转。
        // 用 TransactionSynchronizationManager 注册提交后回调，而不是简单地把发送写在 save 之后，
        // 因为方法体内的"之后"仍然在同一个 @Transactional 边界里，事务可能在方法返回后才真正提交或回滚。
        Long shopId = TenantContext.getRequired();
        Long orderId = order.getId();
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        orderCloseDelayProducer.sendDelayClose(shopId, orderId, payTimeoutMinutes);
                    }
                });

        return order;
    }

    @Override
    public Order findByOrderNo(String orderNo) {
        return this.getOne(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
    }

    @Override
    public boolean recordPayMethod(String orderNo, String payMethod) {
        return this.update(Wrappers.<Order>lambdaUpdate()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getPayStatus, "unpaid")
                .set(Order::getPayMethod, payMethod));
    }

    @Override
    public boolean markPaid(String orderNo, String transactionId, String payMethod) {
        return this.update(Wrappers.<Order>lambdaUpdate()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getPayStatus, "unpaid")
                .set(Order::getPayStatus, "paid")
                .set(Order::getPayMethod, payMethod)
                .set(Order::getTransactionId, transactionId)
                .set(Order::getPayTime, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long orderId, ShipCommand cmd) {
        Order order = this.getByIdWithTenant(orderId);
        if ("pickup".equals(order.getDeliveryType())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "自提订单无需发货，请使用核销码核销");
        }
        if (!"paid".equals(order.getPayStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单尚未支付，不能发货");
        }
        if ("received".equals(order.getDeliveryStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单已确认收货，不能再次发货");
        }

        List<OrderGoods> goods = orderGoodsService.listByOrderId(orderId);
        List<Long> allIds = goods.stream().map(OrderGoods::getId).filter(Objects::nonNull).toList();
        if (allIds.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单没有可发货的商品");
        }
        Set<Long> already = shippedGoodsIds(orderId, allIds);
        List<Long> remaining = allIds.stream().filter(id -> !already.contains(id)).toList();
        if (remaining.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单商品已全部发出");
        }

        List<Long> requested = cmd.orderGoodsIds() == null
                ? List.of()
                : cmd.orderGoodsIds().stream().filter(Objects::nonNull).distinct().toList();
        List<Long> toShip;
        if (requested.isEmpty()) {
            toShip = remaining;
        } else {
            for (Long id : requested) {
                if (!allIds.contains(id)) {
                    throw new BusinessException(ErrorCode.PARAM_INVALID, "发货商品不属于该订单");
                }
                if (already.contains(id)) {
                    throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "所选商品已发货");
                }
            }
            toShip = requested;
        }

        OrderPackage pkg = new OrderPackage();
        pkg.setOrderId(orderId);
        pkg.setExpressCompany(cmd.expressCompany());
        pkg.setExpressNo(cmd.expressNo());
        pkg.setOrderGoodsIds(toJson(toShip));
        orderPackageService.save(pkg);

        // 只有"待发货"流转到"已发货"这一步用乐观锁条件更新；已经是 shipped 时只落包裹，不再改状态。
        this.update(Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getDeliveryStatus, "pending")
                .set(Order::getDeliveryStatus, "shipped"));
    }

    @Override
    public void confirmReceipt(Long orderId) {
        this.getByIdWithTenant(orderId);
        boolean updated = this.update(Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getDeliveryStatus, "shipped")
                .set(Order::getDeliveryStatus, "received")
                .set(Order::getReceiptStatus, "confirmed")
                .set(Order::getOrderStatus, "finished"));
        if (!updated) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单尚未发货，不能确认收货");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long orderId, String reason) {
        // 越权校验必须放在状态条件更新之前且单独判定：不能让"不属于当前租户"和"状态不是unpaid"
        // 共用同一次 0 行判定——否则跨租户取消一个"已支付"订单时，会返回 200+"未生效"而不是 403，
        // 与文档三 §2.4"越权异常必须映射为403"的硬性要求相悖，见 ship()/approve() 同类修复。
        Order order = this.getByIdWithTenant(orderId);
        boolean updated = this.update(Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getPayStatus, "unpaid")
                .set(Order::getOrderStatus, "cancelled")
                .set(Order::getCloseReason, reason));
        if (!updated) {
            // 订单已经不是 unpaid（比如支付成功回调先一步到达），说明这次取消/关单动作应当作废，
            // 不回补库存——库存已经属于一笔已支付的订单，回补会导致超卖。
            return false;
        }
        for (OrderGoods og : orderGoodsService.listByOrderId(orderId)) {
            goodsSkuService.restoreStock(og.getSkuId(), og.getTotalNum());
        }
        // 退回本单核销的优惠券，仅当该券确实由本订单核销时才退回 unused（releaseUsed 带 orderId 条件）
        if (order.getCouponId() != null) {
            userCouponService.release(order.getCouponId(), orderId);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean systemRefund(Long orderId, String reason) {
        // 越权校验单独先行判定，理由同 cancel()
        Order order = this.getByIdWithTenant(orderId);
        boolean updated = this.update(Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getPayStatus, "paid")
                .set(Order::getPayStatus, "refunded")
                .set(Order::getOrderStatus, "cancelled")
                .set(Order::getCloseReason, reason));
        if (!updated) {
            // 不是 paid 状态（未支付应走 cancel，已退款则跳过），调用方据此判断是否需要改走 cancel
            return false;
        }
        for (OrderGoods og : orderGoodsService.listByOrderId(orderId)) {
            goodsSkuService.restoreStock(og.getSkuId(), og.getTotalNum());
        }
        if (order.getCouponId() != null) {
            userCouponService.release(order.getCouponId(), orderId);
        }
        return true;
    }

    @Override
    public List<Order> listByGroupRecordId(Long groupRecordId) {
        return this.list(Wrappers.<Order>lambdaQuery()
                .eq(Order::getGroupRecordId, groupRecordId)
                .orderByAsc(Order::getId));
    }

    /** {yyyyMMdd}{shopId后4位}{雪花后8位}，见文档三 §3.3 订单号规则。 */
    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(DATE_FMT);
        long shopId = TenantContext.getRequired();
        String shopPart = String.format("%04d", shopId % 10000);
        String randomPart = String.format("%08d", ThreadLocalRandom.current().nextInt(100000000));
        return datePart + shopPart + randomPart;
    }

    /**
     * 已发出的订单行。历史数据里空数组表示当时整单发货，按全部行已发出处理。
     */
    private Set<Long> shippedGoodsIds(Long orderId, List<Long> allIds) {
        Set<Long> shipped = new LinkedHashSet<>();
        for (OrderPackage pkg : orderPackageService.listByOrderId(orderId)) {
            List<Long> ids = parseIdList(pkg.getOrderGoodsIds());
            if (ids.isEmpty()) {
                return new LinkedHashSet<>(allIds);
            }
            shipped.addAll(ids);
        }
        return shipped;
    }

    private List<Long> parseIdList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, Long.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "序列化失败");
        }
    }
}
