package com.shopplatform.domain.goods.service;

import com.shopplatform.domain.goods.entity.GoodsComment;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface GoodsCommentService extends TenantSafeService<GoodsComment> {

    /**
     * 发布评价：仅允许 order_goods.receipt 对应订单已确认收货，且该 order_goods 尚未评价过
     * （{@code is_comment=false}）。发布成功后把 order_goods.is_comment 置为 true——
     * 唯一索引 {@code uk_order_goods} 兜底并发重复提交，这里的业务校验是第一道防线。
     */
    GoodsComment publish(PublishCommand command);

    List<GoodsComment> listByGoodsId(Long goodsId);

    record PublishCommand(
            Long orderId,
            Long orderGoodsId,
            Long userId,
            Integer score,
            String content,
            List<String> images
    ) {
    }
}
