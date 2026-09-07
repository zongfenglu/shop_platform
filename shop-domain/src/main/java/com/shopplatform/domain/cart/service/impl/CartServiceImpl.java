package com.shopplatform.domain.cart.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.cart.entity.Cart;
import com.shopplatform.domain.cart.mapper.CartMapper;
import com.shopplatform.domain.cart.service.CartService;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    /** 单个 SKU 在车内的数量上限，防止手滑/脚本把数量撑到离谱的值。 */
    private static final int MAX_QUANTITY_PER_SKU = 200;

    private final GoodsService goodsService;
    private final GoodsSkuService goodsSkuService;

    public CartServiceImpl(GoodsService goodsService, GoodsSkuService goodsSkuService) {
        this.goodsService = goodsService;
        this.goodsSkuService = goodsSkuService;
    }

    @Override
    @Transactional
    public Cart addItem(Long userId, Long skuId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "数量必须大于0");
        }
        GoodsSku sku = goodsSkuService.getByIdWithTenant(skuId);
        Goods goods = goodsService.getByIdWithTenant(sku.getGoodsId());
        if (!"on".equals(goods.getStatus())) {
            throw new BusinessException(ErrorCode.GOODS_OFF_SHELF, "商品「" + goods.getName() + "」已下架");
        }

        Cart existing = this.getOne(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getSkuId, skuId)
                .last("limit 1"));
        int target = (existing == null ? 0 : existing.getQuantity()) + quantity;
        checkQuantity(sku, target);

        if (existing != null) {
            existing.setQuantity(target);
            this.updateById(existing);
            return existing;
        }
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setGoodsId(goods.getId());
        cart.setSkuId(skuId);
        cart.setQuantity(target);
        this.save(cart);
        return cart;
    }

    @Override
    public void updateQuantity(Long userId, Long cartId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "数量必须大于0");
        }
        Cart cart = requireOwnItem(userId, cartId);
        checkQuantity(goodsSkuService.getByIdWithTenant(cart.getSkuId()), quantity);
        cart.setQuantity(quantity);
        this.updateById(cart);
    }

    @Override
    public void removeItems(Long userId, List<Long> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            return;
        }
        // 带 userId 条件的批量删除：不属于当前用户的 id 直接匹配不到，
        // 既做了归属校验又不用先查一遍（也不会因为混入了别人的 id 就整批失败）。
        this.remove(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId)
                .in(Cart::getId, cartIds));
    }

    @Override
    public void clear(Long userId) {
        this.remove(Wrappers.<Cart>lambdaQuery().eq(Cart::getUserId, userId));
    }

    @Override
    public List<Cart> listByUser(Long userId) {
        return this.list(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime));
    }

    @Override
    public int countQuantity(Long userId) {
        return listByUser(userId).stream().mapToInt(Cart::getQuantity).sum();
    }

    /**
     * 加购阶段的库存校验只是前置提示，不锁库存——真正的扣减在下单时由
     * {@code GoodsSkuService.deductStock} 的乐观锁负责（见文档三 §5）。
     * 这里放行了也可能在结算时卖光，这是预期行为，不要在购物车里做预占。
     */
    private void checkQuantity(GoodsSku sku, int target) {
        if (target > MAX_QUANTITY_PER_SKU) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "单个商品最多购买 " + MAX_QUANTITY_PER_SKU + " 件");
        }
        if (sku.getStock() != null && target > sku.getStock()) {
            throw new BusinessException(ErrorCode.SKU_STOCK_INSUFFICIENT, "库存不足，当前仅剩 " + sku.getStock() + " 件");
        }
    }

    private Cart requireOwnItem(Long userId, Long cartId) {
        Cart cart = this.getOne(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getId, cartId)
                .eq(Cart::getUserId, userId));
        if (cart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "购物车记录不存在");
        }
        return cart;
    }
}
