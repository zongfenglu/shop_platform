package com.shopplatform.domain.dealer.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.domain.dealer.entity.DealerOrder;
import com.shopplatform.domain.dealer.entity.DealerSetting;
import com.shopplatform.domain.dealer.entity.DealerUser;
import com.shopplatform.domain.dealer.mapper.DealerOrderMapper;
import com.shopplatform.domain.dealer.service.DealerSettingService;
import com.shopplatform.domain.dealer.service.DealerUserService;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.service.OrderGoodsService;
import com.shopplatform.framework.tenant.TenantContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 佣金计算：整单（order）与按商品（goods）两种 commission_type 的口径差异见
 * {@link DealerOrderServiceImpl#createPending}。
 */
class DealerOrderServiceImplTest {

    private DealerSettingService dealerSettingService;
    private DealerUserService dealerUserService;
    private OrderGoodsService orderGoodsService;
    private GoodsService goodsService;
    private DealerOrderServiceImpl service;

    private static final Long SHOP_ID = 999L;
    private static final Long ORDER_ID = 5001L;
    private static final Long BUYER_ID = 1001L;
    private static final Long PARENT_DEALER_ID = 2002L;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DealerOrder.class);
    }

    @BeforeEach
    void setUp() {
        dealerSettingService = mock(DealerSettingService.class);
        dealerUserService = mock(DealerUserService.class);
        orderGoodsService = mock(OrderGoodsService.class);
        goodsService = mock(GoodsService.class);

        DealerOrderServiceImpl impl = new DealerOrderServiceImpl(
                dealerSettingService, dealerUserService, orderGoodsService, goodsService, new ObjectMapper());
        ReflectionTestUtils.setField(impl, "baseMapper", mock(DealerOrderMapper.class));
        service = spy(impl);
        doReturn(true).when(service).save(any(DealerOrder.class));

        TenantContext.set(SHOP_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private DealerSetting setting(String type, String ratePercent) {
        DealerSetting s = new DealerSetting();
        s.setShopId(SHOP_ID);
        s.setIsEnable(1);
        s.setCommissionType(type);
        s.setCommissionRate(new BigDecimal(ratePercent));
        return s;
    }

    /** 买家自己是分销商且有 active 上级：佣金归上级 */
    private void givenBuyerHasParentDealer() {
        DealerUser self = new DealerUser();
        self.setUserId(BUYER_ID);
        self.setStatus("active");
        self.setParentId(PARENT_DEALER_ID);
        DealerUser parent = new DealerUser();
        parent.setId(PARENT_DEALER_ID);
        parent.setStatus("active");
        when(dealerUserService.getMyDealer(BUYER_ID)).thenReturn(self);
        when(dealerUserService.getByIdWithTenant(PARENT_DEALER_ID)).thenReturn(parent);
    }

    private OrderGoods line(long goodsId, String goodsPrice, int num, String discountDetailJson) {
        OrderGoods og = new OrderGoods();
        og.setGoodsId(goodsId);
        og.setGoodsPrice(new BigDecimal(goodsPrice));
        og.setTotalNum(num);
        og.setDiscountDetail(discountDetailJson);
        return og;
    }

    private Goods goodsWithRate(long id, String ratePercent) {
        Goods g = new Goods();
        g.setId(id);
        g.setCommissionRate(ratePercent == null ? null : new BigDecimal(ratePercent));
        return g;
    }

    private DealerOrder captureSaved() {
        ArgumentCaptor<DealerOrder> captor = ArgumentCaptor.forClass(DealerOrder.class);
        verify(service).save(captor.capture());
        return captor.getValue();
    }

    // ---- 整单佣金 ----

    @Test
    void orderType_commissionIsOrderTotalTimesShopRate() {
        when(dealerSettingService.getOrCreate()).thenReturn(setting("order", "10"));
        givenBuyerHasParentDealer();

        service.createPending(ORDER_ID, BUYER_ID, new BigDecimal("200.00"));

        DealerOrder saved = captureSaved();
        assertEquals(new BigDecimal("20.00"), saved.getCommissionAmount());
        assertEquals(PARENT_DEALER_ID, saved.getDealerUserId());
        assertEquals("pending", saved.getStatus());
        // 整单模式完全不查订单行
        verifyNoInteractions(orderGoodsService, goodsService);
    }

    @Test
    void disabled_noCommission() {
        DealerSetting s = setting("order", "10");
        s.setIsEnable(0);
        when(dealerSettingService.getOrCreate()).thenReturn(s);

        service.createPending(ORDER_ID, BUYER_ID, new BigDecimal("200.00"));

        verify(service, never()).save(any(DealerOrder.class));
    }

    @Test
    void buyerNotDealer_noCommission() {
        when(dealerSettingService.getOrCreate()).thenReturn(setting("order", "10"));
        when(dealerUserService.getMyDealer(BUYER_ID)).thenReturn(null);

        service.createPending(ORDER_ID, BUYER_ID, new BigDecimal("200.00"));

        verify(service, never()).save(any(DealerOrder.class));
    }

    @Test
    void noActiveParent_selfPurchaseCommission() {
        when(dealerSettingService.getOrCreate()).thenReturn(setting("order", "10"));
        DealerUser self = new DealerUser();
        self.setId(3003L);
        self.setUserId(BUYER_ID);
        self.setStatus("active");
        self.setParentId(null);
        when(dealerUserService.getMyDealer(BUYER_ID)).thenReturn(self);

        service.createPending(ORDER_ID, BUYER_ID, new BigDecimal("100.00"));

        assertEquals(3003L, captureSaved().getDealerUserId());
    }

    // ---- 按商品佣金 ----

    @Test
    void goodsType_perLineRate_sumOfLineCommissions() {
        when(dealerSettingService.getOrCreate()).thenReturn(setting("goods", "10"));
        givenBuyerHasParentDealer();
        // 商品 A 单独设 20%，商品 B 未设置 → 回退店铺默认 10%
        when(orderGoodsService.listByOrderId(ORDER_ID)).thenReturn(List.of(
                line(11L, "100.00", 1, null),   // A: 100 × 20% = 20
                line(12L, "50.00", 2, null)));  // B: 100 × 10% = 10
        when(goodsService.listByIds(any())).thenReturn(List.of(
                goodsWithRate(11L, "20"), goodsWithRate(12L, null)));

        service.createPending(ORDER_ID, BUYER_ID, new BigDecimal("200.00"));

        assertEquals(new BigDecimal("30.00"), captureSaved().getCommissionAmount());
    }

    @Test
    void goodsType_discountDetailReducesBase() {
        when(dealerSettingService.getOrCreate()).thenReturn(setting("goods", "10"));
        givenBuyerHasParentDealer();
        // 行实付 = 100×1 − (优惠 20) = 80 → 80 × 10% = 8
        when(orderGoodsService.listByOrderId(ORDER_ID)).thenReturn(List.of(
                line(11L, "100.00", 1, "{\"coupon\":15.00,\"fullReduce\":5.00}")));
        when(goodsService.listByIds(any())).thenReturn(List.of(goodsWithRate(11L, null)));

        service.createPending(ORDER_ID, BUYER_ID, new BigDecimal("80.00"));

        assertEquals(new BigDecimal("8.00"), captureSaved().getCommissionAmount());
    }

    @Test
    void goodsType_zeroRateGoods_skipped_andNoRecordWhenAllZero() {
        when(dealerSettingService.getOrCreate()).thenReturn(setting("goods", "10"));
        givenBuyerHasParentDealer();
        // 单品显式设 0% = 该商品不参与分佣
        when(orderGoodsService.listByOrderId(ORDER_ID)).thenReturn(List.of(line(11L, "100.00", 1, null)));
        when(goodsService.listByIds(any())).thenReturn(List.of(goodsWithRate(11L, "0")));

        service.createPending(ORDER_ID, BUYER_ID, new BigDecimal("100.00"));

        verify(service, never()).save(any(DealerOrder.class));
    }

    @Test
    void goodsType_noOrderLines_fallsBackToOrderLevel() {
        when(dealerSettingService.getOrCreate()).thenReturn(setting("goods", "10"));
        givenBuyerHasParentDealer();
        when(orderGoodsService.listByOrderId(ORDER_ID)).thenReturn(List.of());

        service.createPending(ORDER_ID, BUYER_ID, new BigDecimal("200.00"));

        assertEquals(new BigDecimal("20.00"), captureSaved().getCommissionAmount());
    }

    @Test
    void goodsType_goodsDeletedAfterOrder_usesShopDefaultRate() {
        when(dealerSettingService.getOrCreate()).thenReturn(setting("goods", "10"));
        givenBuyerHasParentDealer();
        // 下单后商品被删（listByIds 查不到）→ 回退店铺默认比例，不因商品消失漏佣金
        when(orderGoodsService.listByOrderId(ORDER_ID)).thenReturn(List.of(line(11L, "100.00", 1, null)));
        when(goodsService.listByIds(any())).thenReturn(List.of());

        service.createPending(ORDER_ID, BUYER_ID, new BigDecimal("100.00"));

        assertEquals(new BigDecimal("10.00"), captureSaved().getCommissionAmount());
    }
}
