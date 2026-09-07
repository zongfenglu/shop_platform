package com.shopplatform.clientapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.clientapi.dto.CartItemRequest;
import com.shopplatform.clientapi.dto.CheckoutRequest;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.cart.service.CartService;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.goods.service.GoodsSpecValueService;
import com.shopplatform.domain.marketing.entity.SeckillActive;
import com.shopplatform.domain.marketing.entity.SeckillGoods;
import com.shopplatform.domain.marketing.service.SeckillActiveService;
import com.shopplatform.domain.marketing.service.SeckillGoodsService;
import com.shopplatform.domain.marketing.service.SeckillStockService;
import com.shopplatform.domain.marketing.service.GroupActiveService;
import com.shopplatform.domain.marketing.service.GroupRecordService;
import com.shopplatform.domain.marketing.service.BargainRecordService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.pricing.PriceCalculator;
import com.shopplatform.framework.security.LoginUserContext;
import com.shopplatform.framework.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CheckoutAppService#submit} 秒杀下单集成单测：
 * 秒杀预扣成功→incrSold、createOrder 失败→回补 Redis、限时折扣不预扣、预扣不足直接拦截。
 */
class CheckoutAppServiceTest {

    private GoodsService goodsService;
    private GoodsSkuService goodsSkuService;
    private GoodsSpecValueService goodsSpecValueService;
    private PriceCalculator priceCalculator;
    private OrderService orderService;
    private CartService cartService;
    private SeckillActiveService seckillActiveService;
    private SeckillGoodsService seckillGoodsService;
    private SeckillStockService seckillStockService;
    private CheckoutAppService service;

    @BeforeEach
    void setUp() {
        goodsService = mock(GoodsService.class);
        goodsSkuService = mock(GoodsSkuService.class);
        goodsSpecValueService = mock(GoodsSpecValueService.class);
        priceCalculator = mock(PriceCalculator.class);
        orderService = mock(OrderService.class);
        cartService = mock(CartService.class);
        seckillActiveService = mock(SeckillActiveService.class);
        seckillGoodsService = mock(SeckillGoodsService.class);
        seckillStockService = mock(SeckillStockService.class);

        service = new CheckoutAppService(goodsService, goodsSkuService, goodsSpecValueService,
                priceCalculator, orderService, cartService,
                seckillActiveService, seckillGoodsService, seckillStockService,
                mock(GroupActiveService.class), mock(GroupRecordService.class),
                mock(BargainRecordService.class), new ObjectMapper());

        TenantContext.set(1001L);
        LoginUserContext.set(new LoginUserContext.LoginUser(9L, 1001L, "13800000000", false));
        stubResolveItem();
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
        TenantContext.clear();
    }

    private void stubResolveItem() {
        GoodsSku sku = new GoodsSku();
        sku.setId(11L);
        sku.setGoodsId(21L);
        sku.setPrice(new BigDecimal("100.00"));
        sku.setLinePrice(new BigDecimal("120.00"));
        sku.setSpecValueIds("");
        when(goodsSkuService.getByIdWithTenant(11L)).thenReturn(sku);
        Goods goods = new Goods();
        goods.setId(21L);
        goods.setName("秒杀商品A");
        goods.setStatus("on");
        goods.setImages("[\"img.jpg\"]");
        goods.setCategoryIds("[10]");
        when(goodsService.getByIdWithTenant(21L)).thenReturn(goods);
        when(goodsSpecValueService.listByIds(any())).thenReturn(List.of());
    }

    private CheckoutRequest seckillRequest(String timeIds) {
        return new CheckoutRequest(List.of(new CartItemRequest(11L, 2)), "express", null, null,
                null, null, "seckill", 1L, null, null, null, List.of());
    }

    private SeckillActive active(String timeIds) {
        SeckillActive a = new SeckillActive();
        a.setId(1L);
        a.setStatus("on");
        a.setStartDate(LocalDate.now());
        a.setEndDate(LocalDate.now());
        a.setTimeIds(timeIds);
        return a;
    }

    private SeckillGoods sg(int limit) {
        SeckillGoods g = new SeckillGoods();
        g.setId(2L);
        g.setActiveId(1L);
        g.setSkuId(11L);
        g.setSeckillPrice(new BigDecimal("80.00"));
        g.setSeckillNum(100);
        g.setLimitPerUser(limit);
        g.setSold(0);
        g.setStatus("on");
        return g;
    }

    @Test
    void submit_seckill_preDeductSuccess_thenIncrSold() {
        when(seckillActiveService.getByIdWithTenant(1L)).thenReturn(active("[10]"));
        when(seckillGoodsService.findByActiveAndSku(1L, 11L)).thenReturn(sg(1));
        when(seckillStockService.preDeduct(eq(9L), eq(1L), eq(11L), eq(2), eq(1)))
                .thenReturn(SeckillStockService.SUCCESS);
        Order created = new Order();
        created.setId(5L);
        when(orderService.createOrder(any())).thenReturn(created);

        Order result = service.submit(seckillRequest("[10]"));

        assertEquals(5L, result.getId());
        verify(seckillStockService).preDeduct(9L, 1L, 11L, 2, 1);
        verify(seckillGoodsService).incrSold(2L, 2);
        verify(seckillStockService, never()).rollback(any(), any(), any(), anyInt());
    }

    @Test
    void submit_seckill_createOrderFails_rollsBackRedis() {
        when(seckillActiveService.getByIdWithTenant(1L)).thenReturn(active("[10]"));
        when(seckillGoodsService.findByActiveAndSku(1L, 11L)).thenReturn(sg(1));
        when(seckillStockService.preDeduct(eq(9L), eq(1L), eq(11L), eq(2), eq(1)))
                .thenReturn(SeckillStockService.SUCCESS);
        when(orderService.createOrder(any())).thenThrow(new RuntimeException("db stock insufficient"));

        assertThrows(RuntimeException.class, () -> service.submit(seckillRequest("[10]")));

        verify(seckillStockService).rollback(9L, 1L, 11L, 2);
        verify(seckillGoodsService, never()).incrSold(any(), anyInt());
    }

    @Test
    void submit_limitedTimeDiscount_noPreDeduct() {
        when(seckillActiveService.getByIdWithTenant(1L)).thenReturn(active(null)); // time_ids 空 = 限时折扣
        Order created = new Order();
        created.setId(6L);
        when(orderService.createOrder(any())).thenReturn(created);

        Order result = service.submit(seckillRequest(null));

        assertEquals(6L, result.getId());
        verify(seckillStockService, never()).preDeduct(any(), any(), any(), anyInt(), anyInt());
        verify(seckillGoodsService, never()).incrSold(any(), anyInt());
    }

    @Test
    void submit_seckill_stockInsufficient_throwsAndNeverCreatesOrder() {
        when(seckillActiveService.getByIdWithTenant(1L)).thenReturn(active("[10]"));
        when(seckillGoodsService.findByActiveAndSku(1L, 11L)).thenReturn(sg(0));
        when(seckillStockService.preDeduct(eq(9L), eq(1L), eq(11L), eq(2), eq(0)))
                .thenReturn(SeckillStockService.STOCK_INSUFFICIENT);

        assertThrows(BusinessException.class, () -> service.submit(seckillRequest("[10]")));

        verify(orderService, never()).createOrder(any());
        verify(seckillGoodsService, never()).incrSold(any(), anyInt());
    }
}
