package com.shopplatform.domain.goods.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.exception.TenantAccessDeniedException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.goods.entity.GoodsComment;
import com.shopplatform.domain.goods.mapper.GoodsCommentMapper;
import com.shopplatform.domain.goods.service.GoodsCommentService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.service.OrderGoodsService;
import com.shopplatform.domain.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GoodsCommentServiceImpl extends ServiceImpl<GoodsCommentMapper, GoodsComment>
        implements GoodsCommentService {

    private final OrderService orderService;
    private final OrderGoodsService orderGoodsService;
    private final ObjectMapper objectMapper;

    public GoodsCommentServiceImpl(OrderService orderService, OrderGoodsService orderGoodsService,
                                    ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.orderGoodsService = orderGoodsService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GoodsComment publish(PublishCommand cmd) {
        Order order = orderService.getByIdWithTenant(cmd.orderId());
        if (!order.getUserId().equals(cmd.userId())) {
            // 同租户内的越权（A用户拿到B用户的订单id）：shop_id自动过滤拦不住，必须映射为403而不是200+错误码，
            // 见 GlobalExceptionHandler 类注释"越权异常必须映射为403"的硬性要求。
            throw new TenantAccessDeniedException("该订单不属于当前用户，无法发布评价");
        }
        if (!"confirmed".equals(order.getReceiptStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单尚未确认收货，不能发布评价");
        }

        OrderGoods orderGoods = orderGoodsService.getByIdWithTenant(cmd.orderGoodsId());
        if (!orderGoods.getOrderId().equals(cmd.orderId())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "订单商品行与订单不匹配");
        }
        if (Boolean.TRUE.equals(orderGoods.getIsComment())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "该商品已评价，不能重复评价");
        }

        GoodsComment comment = new GoodsComment();
        comment.setGoodsId(orderGoods.getGoodsId());
        comment.setOrderId(cmd.orderId());
        comment.setOrderGoodsId(cmd.orderGoodsId());
        comment.setUserId(cmd.userId());
        comment.setScore(cmd.score());
        comment.setContent(cmd.content());
        comment.setImages(toJson(cmd.images() == null ? List.of() : cmd.images()));
        comment.setStatus("show");
        comment.setIsTop(false);
        // uk_order_goods 唯一约束是最终防线：即便这里的校验被并发绕过，重复插入也会在DB层被拒绝。
        this.save(comment);

        orderGoods.setIsComment(true);
        orderGoodsService.updateById(orderGoods);

        return comment;
    }

    @Override
    public List<GoodsComment> listByGoodsId(Long goodsId) {
        return this.list(Wrappers.<GoodsComment>lambdaQuery()
                .eq(GoodsComment::getGoodsId, goodsId)
                .eq(GoodsComment::getStatus, "show")
                .orderByDesc(GoodsComment::getIsTop)
                .orderByDesc(GoodsComment::getCreateTime));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "序列化失败");
        }
    }
}
