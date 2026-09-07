package com.shopplatform.clientapi.controller;

import com.shopplatform.clientapi.dto.AddCartRequest;
import com.shopplatform.clientapi.dto.CartItemView;
import com.shopplatform.clientapi.dto.UpdateCartQuantityRequest;
import com.shopplatform.clientapi.service.CartAppService;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.cart.service.CartService;
import com.shopplatform.framework.security.LoginUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车。对应原型 h5/cart.html。全部接口都需要登录——
 * 游客购物车（本地存储、登录后合并）不在 M1 范围内，未登录时前端直接引导去登录页。
 */
@RestController
@RequestMapping("/api/cart")
public class ConsumerCartController {

    private final CartService cartService;
    private final CartAppService cartAppService;

    public ConsumerCartController(CartService cartService, CartAppService cartAppService) {
        this.cartService = cartService;
        this.cartAppService = cartAppService;
    }

    @GetMapping
    public Result<List<CartItemView>> list() {
        return Result.ok(cartAppService.listView(requireLoginUserId()));
    }

    /** tabbar 角标用，单独一个轻量接口，免得为了拿个数字把整车都组装一遍。 */
    @GetMapping("/count")
    public Result<Integer> count() {
        return Result.ok(cartService.countQuantity(requireLoginUserId()));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody AddCartRequest request) {
        cartService.addItem(requireLoginUserId(), request.skuId(), request.quantity());
        return Result.ok();
    }

    @PutMapping("/{id}/quantity")
    public Result<Void> updateQuantity(@PathVariable Long id, @Valid @RequestBody UpdateCartQuantityRequest request) {
        cartService.updateQuantity(requireLoginUserId(), id, request.quantity());
        return Result.ok();
    }

    @DeleteMapping
    public Result<Void> remove(@RequestBody List<Long> cartIds) {
        cartService.removeItems(requireLoginUserId(), cartIds);
        return Result.ok();
    }

    @DeleteMapping("/all")
    public Result<Void> clear() {
        cartService.clear(requireLoginUserId());
        return Result.ok();
    }

    private Long requireLoginUserId() {
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return loginUser.userId();
    }
}
