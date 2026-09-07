package com.shopplatform.domain.pricing;

/**
 * 价格计算责任链的单个节点。见文档三 §4，8个固定节点见 {@link PriceHandlerType}。
 * <p>
 * 每个 Handler 只负责修改 {@link PriceWorkingState}（当前单价、运费、分摊明细），
 * 不直接返回最终结果——最终结果的组装统一在 {@link PriceCalculator} 里做一次，
 * 避免每个 Handler 都各自拼一份"最终结果"导致口径不一致。
 */
public interface PriceHandler {

    PriceHandlerType type();

    void handle(PriceContext ctx, PriceWorkingState state);
}
