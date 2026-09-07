package com.shopplatform.domain.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 价格计算过程中的可变工作状态，在责任链各 Handler 之间传递。
 * <p>
 * 每个 {@link WorkingItem} 对应一个 {@link PriceContext.PriceItem}，携带：
 * - {@code currentPrice}：经过前面 Handler 处理后的"当前单价"（初始=SKU原价，ActivityPriceHandler 可能整体替换它）
 * - {@code discountDetail}：每一类优惠在本行的分摊金额，key 为优惠类型（coupon/points/fullReduce/...），
 *   这是文档三 §4 要求的"优惠分摊"落地方式，最终会原样写入 order_goods.discount_detail
 */
public final class PriceWorkingState {

    private final List<WorkingItem> items;
    private BigDecimal freightFee = BigDecimal.ZERO;
    /** 满减规则命中后可置为 true，FreightHandler 据此免运费（满X包邮）。 */
    private boolean freeExpress = false;

    public PriceWorkingState(PriceContext ctx) {
        this.items = new ArrayList<>();
        for (PriceContext.PriceItem item : ctx.items()) {
            items.add(new WorkingItem(item));
        }
    }

    public List<WorkingItem> items() {
        return items;
    }

    public BigDecimal freightFee() {
        return freightFee;
    }

    public void setFreightFee(BigDecimal freightFee) {
        this.freightFee = freightFee;
    }

    public boolean isFreeExpress() {
        return freeExpress;
    }

    public void setFreeExpress(boolean freeExpress) {
        this.freeExpress = freeExpress;
    }

    /** 所有购物项的"当前单价×数量"之和，代表本次链路执行到当前节点为止的商品总额。 */
    public BigDecimal currentGoodsTotal() {
        return items.stream()
                .map(WorkingItem::currentSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 原价小计（不受任何优惠影响），FreightHandler 的满额包邮判定用它（避免靠优惠把小计砍到包邮线以下反而触发包邮）。 */
    public BigDecimal originalGoodsTotal() {
        return items.stream()
                .map(i -> i.source().skuPrice().multiply(BigDecimal.valueOf(i.source().quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static final class WorkingItem {
        private final PriceContext.PriceItem source;
        private BigDecimal currentPrice;
        /** key: 优惠类型（activity/fullReduce/coupon/points），value: 本行分摊到的优惠金额 */
        private final Map<String, BigDecimal> discountDetail = new LinkedHashMap<>();

        WorkingItem(PriceContext.PriceItem source) {
            this.source = source;
            this.currentPrice = source.skuPrice();
        }

        public PriceContext.PriceItem source() {
            return source;
        }

        public BigDecimal currentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
        }

        public BigDecimal currentSubtotal() {
            return currentPrice.multiply(BigDecimal.valueOf(source.quantity()));
        }

        public Map<String, BigDecimal> discountDetail() {
            return discountDetail;
        }

        public void addDiscount(String type, BigDecimal amount) {
            discountDetail.merge(type, amount, BigDecimal::add);
        }
    }
}
