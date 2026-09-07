package com.shopplatform.domain.pricing.handler;

import com.shopplatform.domain.pricing.PriceContext;
import com.shopplatform.domain.pricing.PriceHandler;
import com.shopplatform.domain.pricing.PriceHandlerType;
import com.shopplatform.domain.pricing.PriceWorkingState;
import org.springframework.stereotype.Component;

/**
 * 责任链第1节点：原价小计。见文档三 §4。
 * <p>
 * {@link PriceWorkingState} 构造时已经把每个购物项的 currentPrice 初始化为 SKU 原价，
 * 本 Handler 目前不需要做任何修改——它存在的意义是"占住责任链第一个位置"，
 * 让链路结构完整可读，且为将来（如批发阶梯价）需要在这一步介入时有处可放，
 * 不需要因为"暂时什么都不用做"就把这个节点从链路里拿掉。
 */
@Component
public class BasePriceHandler implements PriceHandler {

    @Override
    public PriceHandlerType type() {
        return PriceHandlerType.BASE;
    }

    @Override
    public void handle(PriceContext ctx, PriceWorkingState state) {
        // no-op：currentPrice 已在 PriceWorkingState 构造时初始化为 SKU 原价
    }
}
