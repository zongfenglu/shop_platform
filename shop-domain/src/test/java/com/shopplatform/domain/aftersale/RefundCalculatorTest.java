package com.shopplatform.domain.aftersale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.domain.order.entity.OrderGoods;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link RefundCalculator} 单测。见开发计划 Sprint 6 验收标准："退款按优惠分摊比例逆向计算"，
 * 以及文档三 §4 的口径要求——退款金额必须与下单时 order_goods.discount_detail 记录的分摊结果成比例，
 * 不允许重新走价格引擎算出跟下单不一致的数字。
 */
class RefundCalculatorTest {

    private final RefundCalculator calculator = new RefundCalculator(new ObjectMapper());

    @Test
    void calculate_fullQuantityRefund_noDiscount_refundsEntireLineAmount() {
        OrderGoods orderGoods = orderGoods(new BigDecimal("199.00"), 2, null);

        RefundCalculator.RefundResult result = calculator.calculate(orderGoods, 2);

        assertEquals(new BigDecimal("398.00"), result.refundAmount());
        assertEquals(0, result.refundDetail().size());
    }

    @Test
    void calculate_fullQuantityRefund_withSingleDiscount_refundsWholeDiscountBack() {
        // 下单时该行优惠分摊：coupon 抵扣 5.20 元，totalNum=2，全部退款(refundNum=2)时应全额退回优惠
        OrderGoods orderGoods = orderGoods(new BigDecimal("199.00"), 2, "{\"coupon\":5.20}");

        RefundCalculator.RefundResult result = calculator.calculate(orderGoods, 2);

        // 398.00 - 5.20 = 392.80
        assertEquals(new BigDecimal("392.80"), result.refundAmount());
        assertEquals(new BigDecimal("5.20"), result.refundDetail().get("coupon"));
    }

    @Test
    void calculate_partialQuantityRefund_prorateDiscountByProportion() {
        // totalNum=3，退1件，proportion=1/3；coupon 分摊总额9.00 -> 退款分摊应为 9.00 * 1/3 = 3.00
        OrderGoods orderGoods = orderGoods(new BigDecimal("100.00"), 3, "{\"coupon\":9.00}");

        RefundCalculator.RefundResult result = calculator.calculate(orderGoods, 1);

        assertEquals(new BigDecimal("3.00"), result.refundDetail().get("coupon"));
        // 100.00*1 - 3.00 = 97.00
        assertEquals(new BigDecimal("97.00"), result.refundAmount());
    }

    @Test
    void calculate_multipleDiscountTypes_eachProratedIndependently() {
        OrderGoods orderGoods = orderGoods(new BigDecimal("50.00"), 2, "{\"coupon\":4.00,\"points\":1.00,\"fullReduce\":3.00}");

        RefundCalculator.RefundResult result = calculator.calculate(orderGoods, 1);

        // proportion = 1/2 = 0.5，各项按各自金额独立四舍五入到分
        assertEquals(new BigDecimal("2.00"), result.refundDetail().get("coupon"));
        assertEquals(new BigDecimal("0.50"), result.refundDetail().get("points"));
        assertEquals(new BigDecimal("1.50"), result.refundDetail().get("fullReduce"));
        // 50.00*1 - (2.00+0.50+1.50) = 46.00
        assertEquals(new BigDecimal("46.00"), result.refundAmount());
    }

    @Test
    void calculate_roundingUsesHalfUp_consistentWithRoundingHandler() {
        // totalNum=3，退1件，proportion=1/3 (非有限小数)；coupon=10.00 -> 10.00/3=3.3333... 四舍五入到分=3.33
        OrderGoods orderGoods = orderGoods(new BigDecimal("33.33"), 3, "{\"coupon\":10.00}");

        RefundCalculator.RefundResult result = calculator.calculate(orderGoods, 1);

        assertEquals(new BigDecimal("3.33"), result.refundDetail().get("coupon"));
    }

    @Test
    void calculate_refundNumExceedsTotalNum_shouldThrow() {
        OrderGoods orderGoods = orderGoods(new BigDecimal("100.00"), 2, null);

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(orderGoods, 3));
    }

    @Test
    void calculate_refundNumZeroOrNegative_shouldThrow() {
        OrderGoods orderGoods = orderGoods(new BigDecimal("100.00"), 2, null);

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(orderGoods, 0));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(orderGoods, -1));
    }

    @Test
    void calculate_blankDiscountDetail_treatedAsNoDiscount() {
        OrderGoods orderGoods = orderGoods(new BigDecimal("100.00"), 1, "");

        RefundCalculator.RefundResult result = calculator.calculate(orderGoods, 1);

        assertEquals(new BigDecimal("100.00"), result.refundAmount());
        assertEquals(0, result.refundDetail().size());
    }

    private OrderGoods orderGoods(BigDecimal goodsPrice, int totalNum, String discountDetail) {
        OrderGoods orderGoods = new OrderGoods();
        orderGoods.setGoodsPrice(goodsPrice);
        orderGoods.setTotalNum(totalNum);
        orderGoods.setDiscountDetail(discountDetail);
        return orderGoods;
    }
}
