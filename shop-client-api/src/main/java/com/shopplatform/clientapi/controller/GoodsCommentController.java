package com.shopplatform.clientapi.controller;

import com.shopplatform.clientapi.dto.PublishCommentRequest;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.goods.entity.GoodsComment;
import com.shopplatform.domain.goods.service.GoodsCommentService;
import com.shopplatform.framework.security.LoginUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品评价。发布评价需要登录，查看某商品的评价列表游客也可浏览（对应商品详情页的评价 Tab）。
 */
@RestController
@RequestMapping("/api")
public class GoodsCommentController {

    private final GoodsCommentService goodsCommentService;

    public GoodsCommentController(GoodsCommentService goodsCommentService) {
        this.goodsCommentService = goodsCommentService;
    }

    @PostMapping("/comment")
    public Result<GoodsComment> publish(@Valid @RequestBody PublishCommentRequest request) {
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        GoodsComment comment = goodsCommentService.publish(new GoodsCommentService.PublishCommand(
                request.orderId(), request.orderGoodsId(), loginUser.userId(),
                request.score(), request.content(), request.images()));
        return Result.ok(comment);
    }

    @GetMapping("/goods/{goodsId}/comments")
    public Result<List<GoodsComment>> listByGoods(@PathVariable Long goodsId) {
        return Result.ok(goodsCommentService.listByGoodsId(goodsId));
    }
}
