package com.shopplatform.clientapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.domain.diy.entity.DiyPage;
import com.shopplatform.domain.diy.service.DiyPageService;
import com.shopplatform.domain.diy.service.DiyTabbarService;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.marketing.entity.BargainActive;
import com.shopplatform.domain.marketing.entity.Coupon;
import com.shopplatform.domain.marketing.entity.GroupActive;
import com.shopplatform.domain.marketing.entity.SeckillActive;
import com.shopplatform.domain.marketing.entity.SeckillGoods;
import com.shopplatform.domain.marketing.service.BargainActiveService;
import com.shopplatform.domain.marketing.service.CouponService;
import com.shopplatform.domain.marketing.service.GroupActiveService;
import com.shopplatform.domain.marketing.service.SeckillActiveService;
import com.shopplatform.domain.marketing.service.SeckillGoodsService;
import com.shopplatform.domain.marketing.service.SeckillStockService;
import com.shopplatform.domain.offlinestore.service.OfflineStoreService;
import com.shopplatform.domain.shop.service.PackageFeatureChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link DiyRenderAppService} 单测：每种营销组件类型至少一个"正常解析"用例 +
 * 一个"引用的活动/商品不存在时静默跳过"用例，照抄 {@code CheckoutAppServiceTest} 的构造器注入mock套路。
 */
class DiyRenderAppServiceTest {

    private static final Long SHOP_ID = 1001L;

    private DiyPageService diyPageService;
    private DiyTabbarService diyTabbarService;
    private GoodsService goodsService;
    private GoodsSkuService goodsSkuService;
    private CouponService couponService;
    private SeckillActiveService seckillActiveService;
    private SeckillGoodsService seckillGoodsService;
    private SeckillStockService seckillStockService;
    private GroupActiveService groupActiveService;
    private BargainActiveService bargainActiveService;
    private PackageFeatureChecker packageFeatureChecker;
    private DiyRenderAppService service;

    @BeforeEach
    void setUp() {
        diyPageService = mock(DiyPageService.class);
        diyTabbarService = mock(DiyTabbarService.class);
        goodsService = mock(GoodsService.class);
        goodsSkuService = mock(GoodsSkuService.class);
        couponService = mock(CouponService.class);
        seckillActiveService = mock(SeckillActiveService.class);
        seckillGoodsService = mock(SeckillGoodsService.class);
        seckillStockService = mock(SeckillStockService.class);
        groupActiveService = mock(GroupActiveService.class);
        bargainActiveService = mock(BargainActiveService.class);
        packageFeatureChecker = mock(PackageFeatureChecker.class);
        when(packageFeatureChecker.hasMenu(any(), any())).thenReturn(true);

        service = new DiyRenderAppService(diyPageService, diyTabbarService, goodsService, goodsSkuService,
                couponService, seckillActiveService, seckillGoodsService, seckillStockService,
                groupActiveService, bargainActiveService, mock(OfflineStoreService.class),
                packageFeatureChecker, new ObjectMapper());
    }

    private DiyPage pageWithData(String pageData) {
        DiyPage page = new DiyPage();
        page.setId(1L);
        page.setShopId(SHOP_ID);
        page.setPageType("home");
        page.setName("首页");
        page.setVersion(1);
        page.setPageData(pageData);
        return page;
    }

    @Test
    void renderHome_includesPageBackground() {
        String pageData = "{\"page\":{\"bgType\":\"image\",\"bgColor\":\"#fff5f0\",\"bgImage\":\"/uploads/a.jpg\"},\"items\":[]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));

        Map<String, Object> result = service.renderHome(SHOP_ID);

        assertEquals(true, result.get("exists"));
        Map<?, ?> page = (Map<?, ?>) result.get("page");
        assertEquals("image", page.get("bgType"));
        assertEquals("#fff5f0", page.get("bgColor"));
        assertEquals("/uploads/a.jpg", page.get("bgImage"));
        assertTrue(((List<?>) result.get("items")).isEmpty());
    }

    @Test
    void renderHome_noHomePage_returnsExistsFalse() {
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(null);

        Map<String, Object> result = service.renderHome(SHOP_ID);

        assertEquals(false, result.get("exists"));
    }

    @Test
    void renderPage_unpublished_throwsNotFound() {
        DiyPage draftOnly = new DiyPage();
        draftOnly.setId(1L);
        draftOnly.setPageData(null);
        when(diyPageService.getByIdWithTenant(1L)).thenReturn(draftOnly);

        assertThrows(BusinessException.class, () -> service.renderPage(SHOP_ID, 1L));
    }

    @Test
    void renderPage_crossShop_throwsNotFound() {
        when(diyPageService.getByIdWithTenant(1L))
                .thenThrow(new com.shopplatform.common.exception.TenantAccessDeniedException("id=1"));

        assertThrows(BusinessException.class, () -> service.renderPage(SHOP_ID, 1L));
    }

    @Test
    void resolveGoods_onSale_returnsGoodsListWithCheapestSku() {
        String pageData = "{\"items\":[{\"type\":\"goods\",\"goodsIds\":[21]}]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));

        Goods goods = new Goods();
        goods.setId(21L);
        goods.setName("商品A");
        goods.setStatus("on");
        goods.setImages("[\"a.jpg\"]");
        when(goodsService.listByIds(List.of(21L))).thenReturn(List.of(goods));

        GoodsSku sku = new GoodsSku();
        sku.setId(11L);
        sku.setGoodsId(21L);
        sku.setPrice(new BigDecimal("50.00"));
        sku.setLinePrice(new BigDecimal("80.00"));
        when(goodsSkuService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(sku));

        Map<String, Object> result = service.renderHome(SHOP_ID);
        List<?> items = (List<?>) result.get("items");
        assertEquals(1, items.size());
        Map<?, ?> goodsItem = (Map<?, ?>) items.get(0);
        List<?> goodsList = (List<?>) goodsItem.get("goodsList");
        assertEquals(1, goodsList.size());
        Map<?, ?> gv = (Map<?, ?>) goodsList.get(0);
        assertEquals(new BigDecimal("50.00"), gv.get("price"));
    }

    @Test
    void resolveGoods_deletedGoods_omittedFromList() {
        String pageData = "{\"items\":[{\"type\":\"goods\",\"goodsIds\":[21]}]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));
        when(goodsService.listByIds(List.of(21L))).thenReturn(List.of());
        when(goodsSkuService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());

        Map<String, Object> result = service.renderHome(SHOP_ID);
        List<?> items = (List<?>) result.get("items");
        Map<?, ?> goodsItem = (Map<?, ?>) items.get(0);
        assertTrue(((List<?>) goodsItem.get("goodsList")).isEmpty());
    }

    @Test
    void resolveCoupon_receivable_returnsFilteredList() {
        String pageData = "{\"items\":[{\"type\":\"coupon\",\"couponIds\":[5]}]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));

        Coupon c1 = new Coupon();
        c1.setId(5L);
        c1.setName("满减券");
        Coupon c2 = new Coupon();
        c2.setId(6L);
        c2.setName("其他券");
        when(couponService.listReceivable()).thenReturn(List.of(c1, c2));

        Map<String, Object> result = service.renderHome(SHOP_ID);
        Map<?, ?> item = (Map<?, ?>) ((List<?>) result.get("items")).get(0);
        List<?> couponList = (List<?>) item.get("couponList");
        assertEquals(1, couponList.size());
        assertEquals(5L, ((Map<?, ?>) couponList.get(0)).get("id"));
    }

    @Test
    void resolveCoupon_preservesConfiguredOrder() {
        String pageData = "{\"items\":[{\"type\":\"coupon\",\"couponIds\":[6,5]}]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));

        Coupon firstFromDatabase = new Coupon();
        firstFromDatabase.setId(5L);
        Coupon secondFromDatabase = new Coupon();
        secondFromDatabase.setId(6L);
        when(couponService.listReceivable()).thenReturn(List.of(firstFromDatabase, secondFromDatabase));

        Map<String, Object> result = service.renderHome(SHOP_ID);
        Map<?, ?> item = (Map<?, ?>) ((List<?>) result.get("items")).get(0);
        List<?> couponList = (List<?>) item.get("couponList");

        assertEquals(6L, ((Map<?, ?>) couponList.get(0)).get("id"));
        assertEquals(5L, ((Map<?, ?>) couponList.get(1)).get("id"));
    }

    @Test
    void resolveVideo_keepsPlaybackConfiguration() {
        String pageData = "{\"items\":[{\"type\":\"video\",\"url\":\"/uploads/demo.mp4\",\"cover\":\"/uploads/demo.jpg\",\"autoplay\":true,\"height\":260,\"margin\":3000}]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));

        Map<String, Object> result = service.renderHome(SHOP_ID);
        Map<?, ?> video = (Map<?, ?>) ((List<?>) result.get("items")).get(0);

        assertEquals("video", video.get("type"));
        assertEquals("/uploads/demo.mp4", video.get("url"));
        assertEquals("/uploads/demo.jpg", video.get("cover"));
        assertEquals(true, video.get("autoplay"));
        assertEquals(260, video.get("height"));
        assertEquals(80, video.get("margin"));
    }

    @Test
    void resolveSeckill_activeOnSale_returnsGoodsWithRemainingStock() {
        String pageData = "{\"items\":[{\"type\":\"seckill\",\"activeId\":1}]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));

        SeckillActive active = new SeckillActive();
        active.setId(1L);
        active.setName("秒杀场");
        when(seckillActiveService.listOnSale()).thenReturn(List.of(active));

        SeckillGoods sg = new SeckillGoods();
        sg.setGoodsId(21L);
        sg.setSkuId(11L);
        sg.setSeckillPrice(new BigDecimal("10.00"));
        sg.setSeckillNum(100);
        sg.setSold(20);
        sg.setStatus("on");
        when(seckillGoodsService.listByActive(1L)).thenReturn(List.of(sg));

        GoodsSku sku = new GoodsSku();
        sku.setId(11L);
        sku.setPrice(new BigDecimal("30.00"));
        sku.setImage("sku.jpg");
        when(goodsSkuService.getByIdWithTenant(11L)).thenReturn(sku);
        Goods goods = new Goods();
        goods.setId(21L);
        goods.setName("秒杀商品");
        when(goodsService.getByIdWithTenant(21L)).thenReturn(goods);
        when(seckillStockService.getStock(1L, 11L)).thenReturn(null);

        Map<String, Object> result = service.renderHome(SHOP_ID);
        Map<?, ?> item = (Map<?, ?>) ((List<?>) result.get("items")).get(0);
        Map<?, ?> active2 = (Map<?, ?>) item.get("active");
        List<?> goodsViews = (List<?>) active2.get("goods");
        assertEquals(1, goodsViews.size());
        assertEquals(80, ((Map<?, ?>) goodsViews.get(0)).get("remaining"));
    }

    @Test
    void resolveSeckill_activeDeletedOrOffSale_itemSkippedSilently() {
        String pageData = "{\"items\":[{\"type\":\"seckill\",\"activeId\":99},{\"type\":\"search\"}]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));
        when(seckillActiveService.listOnSale()).thenReturn(List.of());

        Map<String, Object> result = service.renderHome(SHOP_ID);
        List<?> items = (List<?>) result.get("items");
        assertEquals(1, items.size());
        assertEquals("search", ((Map<?, ?>) items.get(0)).get("type"));
    }

    @Test
    void resolveGroup_activeOnSale_returnsMinGroupPrice() {
        String pageData = "{\"items\":[{\"type\":\"group\",\"activeId\":2}]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));

        GroupActive active = new GroupActive();
        active.setId(2L);
        active.setGoodsId(21L);
        active.setGroupNum(3);
        when(groupActiveService.listOnSale()).thenReturn(List.of(active));
        when(groupActiveService.parseGroupPrice(active)).thenReturn(Map.of(11L, new BigDecimal("60.00")));
        Goods goods = new Goods();
        goods.setId(21L);
        goods.setName("拼团商品");
        when(goodsService.getByIdWithTenant(21L)).thenReturn(goods);

        Map<String, Object> result = service.renderHome(SHOP_ID);
        Map<?, ?> item = (Map<?, ?>) ((List<?>) result.get("items")).get(0);
        Map<?, ?> av = (Map<?, ?>) item.get("active");
        assertEquals(new BigDecimal("60.00"), av.get("minGroupPrice"));
    }

    @Test
    void resolveGroup_activeNotFound_returnsEmptyList() {
        String pageData = "{\"items\":[{\"type\":\"group\",\"activeId\":99}]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));
        when(groupActiveService.listOnSale()).thenReturn(List.of());

        Map<String, Object> result = service.renderHome(SHOP_ID);
        Map<?, ?> item = (Map<?, ?>) ((List<?>) result.get("items")).get(0);
        assertTrue(((List<?>) item.get("list")).isEmpty());
    }

    @Test
    void resolveBargain_activeOnSale_returnsFloorPrice() {
        String pageData = "{\"items\":[{\"type\":\"bargain\",\"activeId\":3}]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));

        BargainActive active = new BargainActive();
        active.setId(3L);
        active.setGoodsId(21L);
        active.setFloorPrice(new BigDecimal("20.00"));
        when(bargainActiveService.listOnSale()).thenReturn(List.of(active));
        Goods goods = new Goods();
        goods.setId(21L);
        goods.setName("砍价商品");
        when(goodsService.getByIdWithTenant(21L)).thenReturn(goods);

        Map<String, Object> result = service.renderHome(SHOP_ID);
        Map<?, ?> item = (Map<?, ?>) ((List<?>) result.get("items")).get(0);
        Map<?, ?> av = (Map<?, ?>) item.get("active");
        assertEquals(new BigDecimal("20.00"), av.get("floorPrice"));
    }

    @Test
    void resolveBargain_activeNotFound_returnsEmptyList() {
        String pageData = "{\"items\":[{\"type\":\"bargain\",\"activeId\":99}]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));
        when(bargainActiveService.listOnSale()).thenReturn(List.of());

        Map<String, Object> result = service.renderHome(SHOP_ID);
        Map<?, ?> item = (Map<?, ?>) ((List<?>) result.get("items")).get(0);
        assertTrue(((List<?>) item.get("list")).isEmpty());
    }

    @Test
    void resolveItem_packageLocked_itemSkippedSilently() {
        String pageData = "{\"items\":[{\"type\":\"seckill\",\"activeId\":1}]}";
        when(diyPageService.getDefaultHome(SHOP_ID)).thenReturn(pageWithData(pageData));
        when(packageFeatureChecker.hasMenu(SHOP_ID, "marketing.seckill")).thenReturn(false);

        Map<String, Object> result = service.renderHome(SHOP_ID);
        assertTrue(((List<?>) result.get("items")).isEmpty());
    }
}
