package com.shopplatform.clientapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.clientapi.dto.CartItemRequest;
import com.shopplatform.clientapi.dto.CheckoutRequest;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.cart.service.CartService;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.entity.GoodsSku;
import com.shopplatform.domain.goods.entity.GoodsSpecValue;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.goods.service.GoodsSkuService;
import com.shopplatform.domain.goods.service.GoodsSpecValueService;
import com.shopplatform.domain.marketing.entity.SeckillActive;
import com.shopplatform.domain.marketing.entity.SeckillGoods;
import com.shopplatform.domain.marketing.entity.GroupActive;
import com.shopplatform.domain.marketing.entity.GroupRecord;
import com.shopplatform.domain.marketing.service.SeckillActiveService;
import com.shopplatform.domain.marketing.service.SeckillGoodsService;
import com.shopplatform.domain.marketing.service.SeckillStockService;
import com.shopplatform.domain.marketing.service.GroupActiveService;
import com.shopplatform.domain.marketing.service.GroupRecordService;
import com.shopplatform.domain.marketing.service.BargainRecordService;
import com.shopplatform.domain.order.service.OrderService;
import com.shopplatform.domain.order.entity.Order;
import com.shopplatform.domain.pricing.OrderPriceResult;
import com.shopplatform.domain.pricing.PriceCalculator;
import com.shopplatform.domain.pricing.PriceContext;
import com.shopplatform.framework.security.LoginUserContext;
import com.shopplatform.framework.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车结算的应用层编排：把客户端提交的 (skuId, quantity) 解析成价格引擎认识的 {@link PriceContext.PriceItem}，
 * 商品名称/价格一律以服务端 SKU 记录为准（不采信客户端回传的价格），再交给 {@link PriceCalculator} / {@link OrderService}。
 * 预览和提交必须复用同一套 items 解析逻辑，否则会出现"预览一个价、提交又算出另一个价"。
 */
@Service
public class CheckoutAppService {

    private final GoodsService goodsService;
    private final GoodsSkuService goodsSkuService;
    private final GoodsSpecValueService goodsSpecValueService;
    private final PriceCalculator priceCalculator;
    private final OrderService orderService;
    private final CartService cartService;
    private final SeckillActiveService seckillActiveService;
    private final SeckillGoodsService seckillGoodsService;
    private final SeckillStockService seckillStockService;
    private final GroupActiveService groupActiveService;
    private final GroupRecordService groupRecordService;
    private final BargainRecordService bargainRecordService;
    private final ObjectMapper objectMapper;

    public CheckoutAppService(GoodsService goodsService,
                               GoodsSkuService goodsSkuService,
                               GoodsSpecValueService goodsSpecValueService,
                               PriceCalculator priceCalculator,
                               OrderService orderService,
                               CartService cartService,
                               SeckillActiveService seckillActiveService,
                               SeckillGoodsService seckillGoodsService,
                               SeckillStockService seckillStockService,
                               GroupActiveService groupActiveService,
                               GroupRecordService groupRecordService,
                               BargainRecordService bargainRecordService,
                               ObjectMapper objectMapper) {
        this.goodsService = goodsService;
        this.goodsSkuService = goodsSkuService;
        this.goodsSpecValueService = goodsSpecValueService;
        this.priceCalculator = priceCalculator;
        this.orderService = orderService;
        this.cartService = cartService;
        this.seckillActiveService = seckillActiveService;
        this.seckillGoodsService = seckillGoodsService;
        this.seckillStockService = seckillStockService;
        this.groupActiveService = groupActiveService;
        this.groupRecordService = groupRecordService;
        this.bargainRecordService = bargainRecordService;
        this.objectMapper = objectMapper;
    }

    public OrderPriceResult preview(CheckoutRequest request) {
        return priceCalculator.calculate(buildPriceContext(request));
    }

    @Transactional
    public Order submit(CheckoutRequest request) {
        LoginUserContext.LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录再下单");
        }

        List<PriceContext.PriceItem> items = buildPriceContext(request).items();

        OrderService.AddressInfo addressInfo = request.address() == null ? null : new OrderService.AddressInfo(
                request.address().name(), request.address().phone(), request.address().province(),
                request.address().city(), request.address().region(), request.address().detail());

        // 秒杀（time_ids 非空）：先 Redis 原子预扣限量+限购，拦掉绝大部分流量；限时折扣（time_ids 空）走普通库存不预扣。
        boolean seckill = isSeckillMode(request);
        List<Deducted> deducted = new ArrayList<>();
        if (seckill) {
            preDeductSeckill(request, loginUser.userId(), deducted);
        }

        // 拼团：开团（groupRecordId=null）创建 group_record；参团（groupRecordId 非空）actual_num+1。
        // 拼团记录的写入与下单在同一事务，createOrder 失败则一并回滚，无需手工补偿。
        GroupContext groupCtx = null;
        if ("group".equals(request.activityType()) && request.activityId() != null) {
            groupCtx = prepareGroup(request, loginUser.userId());
        }

        try {
            Order order = orderService.createOrder(new OrderService.CreateOrderCommand(
                    loginUser.userId(), items, request.deliveryType(), request.pickupStoreId(), request.freightTemplateId(),
                    request.couponId(), request.pointsToUse(), request.activityType(), request.activityId(),
                    groupCtx == null ? null : groupCtx.recordId(),
                    "h5", request.buyerRemark(), addressInfo));

            // 开团后回填团长订单 id 到 group_record
            if (groupCtx != null && groupCtx.isLeader()) {
                groupRecordService.setLeaderOrder(groupCtx.recordId(), order.getId());
            }

            // 砍价下单：把砍价记录置为 ordered 并关联订单（ActivityPriceHandler 已校验记录可用）
            if ("bargain".equals(request.activityType()) && request.activityId() != null) {
                bargainRecordService.markOrdered(request.activityId(), order.getId());
            }

            // 下单成功后累加秒杀已售（与 createOrder 同事务，原子落库）
            if (seckill) {
                for (Deducted d : deducted) {
                    seckillGoodsService.incrSold(d.seckillGoodsId(), d.qty());
                }
            }

            // 清掉本单来源的购物车行。removeItems 带 userId 条件，前端传了别人的 cartId 也只会匹配不到、静默忽略。
            if (request.cartIds() != null && !request.cartIds().isEmpty()) {
                cartService.removeItems(loginUser.userId(), request.cartIds());
            }
            return order;
        } catch (RuntimeException e) {
            // createOrder 失败（如 DB 库存不足）或后续异常：回补 Redis 预扣，事务回滚
            if (seckill) {
                for (Deducted d : deducted) {
                    seckillStockService.rollback(loginUser.userId(), request.activityId(), d.skuId(), d.qty());
                }
            }
            throw e;
        }
    }

    /** 拼团预处理：开团创建 pending 记录；参团 actual_num+1（已成团/已结束抛异常）。返回记录 id 与是否团长。 */
    private GroupContext prepareGroup(CheckoutRequest request, Long userId) {
        if (request.groupRecordId() == null) {
            // 开团
            GroupActive active = groupActiveService.getByIdWithTenant(request.activityId());
            GroupRecord rec = groupRecordService.openGroup(active.getId(), userId, null,
                    active.getValidHours() == null ? 24 : active.getValidHours());
            return new GroupContext(rec.getId(), true);
        }
        // 参团
        GroupRecord rec = groupRecordService.getByIdWithTenant(request.groupRecordId());
        GroupActive active = groupActiveService.getByIdWithTenant(rec.getActiveId());
        GroupRecord updated = groupRecordService.joinGroup(rec.getId(), active.getGroupNum());
        if (updated == null) {
            throw new BusinessException(ErrorCode.GROUP_FULL, "拼团已满或已结束");
        }
        return new GroupContext(rec.getId(), false);
    }

    private record GroupContext(Long recordId, boolean isLeader) {
    }

    /** 是否走秒杀预扣：activityType=seckill 且活动 time_ids 非空（限时折扣不预扣）。 */
    private boolean isSeckillMode(CheckoutRequest request) {
        if (!"seckill".equals(request.activityType()) || request.activityId() == null) {
            return false;
        }
        SeckillActive active = seckillActiveService.getByIdWithTenant(request.activityId());
        if (active == null || !"on".equals(active.getStatus())) {
            throw new BusinessException(ErrorCode.ACTIVITY_EXPIRED, "活动已结束");
        }
        return StringUtils.hasText(active.getTimeIds()) && !"[]".equals(active.getTimeIds());
    }

    private void preDeductSeckill(CheckoutRequest request, Long userId, List<Deducted> deducted) {
        for (CartItemRequest ci : request.items()) {
            SeckillGoods sg = seckillGoodsService.findByActiveAndSku(request.activityId(), ci.skuId());
            if (sg == null) {
                rollbackDeducted(request, userId, deducted);
                throw new BusinessException(ErrorCode.SECKILL_GOODS_NOT_FOUND, "秒杀商品不存在");
            }
            int limit = sg.getLimitPerUser() == null ? 0 : sg.getLimitPerUser();
            int code = seckillStockService.preDeduct(userId, request.activityId(), ci.skuId(), ci.quantity(), limit);
            if (code != SeckillStockService.SUCCESS) {
                rollbackDeducted(request, userId, deducted);
                if (code == SeckillStockService.LIMIT_EXCEEDED) {
                    throw new BusinessException(ErrorCode.SECKILL_LIMIT_EXCEEDED, "超出秒杀限购数量");
                }
                throw new BusinessException(ErrorCode.SKU_STOCK_INSUFFICIENT, "秒杀库存不足");
            }
            deducted.add(new Deducted(ci.skuId(), ci.quantity(), sg.getId()));
        }
    }

    private void rollbackDeducted(CheckoutRequest request, Long userId, List<Deducted> deducted) {
        for (Deducted d : deducted) {
            seckillStockService.rollback(userId, request.activityId(), d.skuId(), d.qty());
        }
    }

    /** 已预扣的秒杀行（用于成功后 incrSold / 失败后回补）。 */
    private record Deducted(Long skuId, int qty, Long seckillGoodsId) {
    }

    private PriceContext buildPriceContext(CheckoutRequest request) {
        List<PriceContext.PriceItem> items = request.items().stream()
                .map(this::resolveItem)
                .toList();
        return new PriceContext(TenantContext.getRequired(),
                LoginUserContext.get() == null ? null : LoginUserContext.get().userId(),
                items, request.deliveryType(), request.freightTemplateId(), request.couponId(),
                request.pointsToUse(), request.activityType(), request.activityId());
    }

    private PriceContext.PriceItem resolveItem(CartItemRequest cartItem) {
        GoodsSku sku = goodsSkuService.getByIdWithTenant(cartItem.skuId());
        Goods goods = goodsService.getByIdWithTenant(sku.getGoodsId());
        if (!"on".equals(goods.getStatus())) {
            throw new BusinessException(ErrorCode.GOODS_OFF_SHELF, "商品「" + goods.getName() + "」已下架");
        }
        String image = StringUtils.hasText(sku.getImage()) ? sku.getImage() : firstImage(goods.getImages());
        return new PriceContext.PriceItem(goods.getId(), sku.getId(), goods.getName(),
                resolveSpecText(sku.getSpecValueIds()), image, sku.getPrice(), sku.getLinePrice(),
                cartItem.quantity(), sku.getWeight(), sku.getVolume(), parseCategoryIds(goods.getCategoryIds()));
    }

    /** goods.category_ids 存的是 JSON 数组（如 "[10,11]"），优惠券按分类适用时需要它判定参与项。 */
    private List<Long> parseCategoryIds(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, Long.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private String resolveSpecText(String specValueIds) {
        if (!StringUtils.hasText(specValueIds)) {
            return "";
        }
        List<Long> ids = Arrays.stream(specValueIds.split("_")).map(Long::valueOf).toList();
        List<GoodsSpecValue> values = goodsSpecValueService.listByIds(ids);
        Map<Long, String> byId = values.stream()
                .collect(Collectors.toMap(GoodsSpecValue::getId, GoodsSpecValue::getValue));
        return ids.stream().map(id -> byId.getOrDefault(id, "")).collect(Collectors.joining("/"));
    }

    private String firstImage(String imagesJson) {
        if (!StringUtils.hasText(imagesJson)) {
            return "";
        }
        try {
            List<String> images = objectMapper.readValue(imagesJson,
                    objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class));
            return images.isEmpty() ? "" : images.get(0);
        } catch (Exception e) {
            return "";
        }
    }
}
