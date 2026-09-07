package com.shopplatform.domain.pricing;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;

/**
 * 价格计算引擎的单一入口。见文档三 §4：
 * "下单预览、正式下单、修改订单、售后退款金额计算——必须共用一套代码。
 * 任何一处单独算价，最终都会出现'预览98元、实付102元'的投诉。"
 * <p>
 * 本类是唯一允许调用责任链的地方；业务代码（下单/预览/退款）都只应依赖 {@link #calculate(PriceContext)}，
 * 不允许绕过它自己拼算价逻辑。
 */
@Component
public class PriceCalculator {

    private final EnumMap<PriceHandlerType, PriceHandler> handlerByType = new EnumMap<>(PriceHandlerType.class);

    public PriceCalculator(List<PriceHandler> handlers) {
        for (PriceHandler handler : handlers) {
            PriceHandler existing = handlerByType.put(handler.type(), handler);
            if (existing != null) {
                throw new IllegalStateException(
                        "PriceHandlerType." + handler.type() + " 被重复注册（" + existing.getClass().getSimpleName()
                                + " 与 " + handler.getClass().getSimpleName() + "），责任链每个节点只允许一个实现");
            }
        }
    }

    public OrderPriceResult calculate(PriceContext ctx) {
        if (ctx.items() == null || ctx.items().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "购物项不能为空");
        }

        PriceWorkingState state = new PriceWorkingState(ctx);

        // 按 PriceHandlerType 枚举的固定 ordinal 顺序执行，不依赖 Spring 注入顺序。
        // 见文档三 §4："责任链顺序（固定，不可配置乱序）"；某个类型暂未落地实现（M2 才做）就跳过，
        // 不阻断整条链——这正是 Sprint 4 只落地 BASE/FREIGHT/ROUNDING 三个节点也能正常工作的原因。
        for (PriceHandlerType type : PriceHandlerType.values()) {
            PriceHandler handler = handlerByType.get(type);
            if (handler != null) {
                handler.handle(ctx, state);
            }
        }

        return assembleResult(state);
    }

    private OrderPriceResult assembleResult(PriceWorkingState state) {
        BigDecimal totalPrice = state.originalGoodsTotal();
        BigDecimal discountPrice = sumDiscountByPrefix(state, "activity", "fullReduce", "memberDiscount");
        BigDecimal couponPrice = sumDiscountByPrefix(state, "coupon");
        BigDecimal pointsPrice = sumDiscountByPrefix(state, "points");
        BigDecimal expressPrice = state.freightFee();

        BigDecimal payPrice = totalPrice
                .subtract(discountPrice)
                .subtract(couponPrice)
                .subtract(pointsPrice)
                .add(expressPrice)
                .setScale(2, RoundingMode.HALF_UP);

        List<OrderPriceResult.ItemResult> itemResults = state.items().stream()
                .map(item -> new OrderPriceResult.ItemResult(
                        item.source().goodsId(),
                        item.source().skuId(),
                        item.source().goodsName(),
                        item.source().specText(),
                        item.source().image(),
                        item.source().skuPrice(),
                        item.source().linePrice(),
                        item.source().quantity(),
                        item.source().skuPrice().multiply(BigDecimal.valueOf(item.source().quantity())),
                        item.discountDetail()
                ))
                .toList();

        return new OrderPriceResult(totalPrice, discountPrice, couponPrice, pointsPrice, expressPrice, payPrice, itemResults);
    }

    private BigDecimal sumDiscountByPrefix(PriceWorkingState state, String... keys) {
        BigDecimal sum = BigDecimal.ZERO;
        for (PriceWorkingState.WorkingItem item : state.items()) {
            for (String key : keys) {
                BigDecimal v = item.discountDetail().get(key);
                if (v != null) {
                    sum = sum.add(v);
                }
            }
        }
        return sum;
    }
}
