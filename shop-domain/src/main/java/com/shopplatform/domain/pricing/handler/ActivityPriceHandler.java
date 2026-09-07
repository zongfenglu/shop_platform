package com.shopplatform.domain.pricing.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.marketing.entity.BargainActive;
import com.shopplatform.domain.marketing.entity.BargainRecord;
import com.shopplatform.domain.marketing.entity.GroupActive;
import com.shopplatform.domain.marketing.entity.SeckillActive;
import com.shopplatform.domain.marketing.entity.SeckillGoods;
import com.shopplatform.domain.marketing.entity.SeckillTime;
import com.shopplatform.domain.marketing.service.BargainActiveService;
import com.shopplatform.domain.marketing.service.BargainRecordService;
import com.shopplatform.domain.marketing.service.GroupActiveService;
import com.shopplatform.domain.marketing.service.SeckillActiveService;
import com.shopplatform.domain.marketing.service.SeckillGoodsService;
import com.shopplatform.domain.marketing.service.SeckillTimeService;
import com.shopplatform.domain.pricing.PriceContext;
import com.shopplatform.domain.pricing.PriceHandler;
import com.shopplatform.domain.pricing.PriceHandlerType;
import com.shopplatform.domain.pricing.PriceWorkingState;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 责任链第2节点：秒杀 / 限时折扣 / 拼团 / 砍价（互斥）。见文档三 §4。
 * <p>
 * 由 ctx.activityType 决定走哪条分支，activityId 语义随类型而变：
 * <ul>
 *   <li>seckill：activityId = seckill_active.id，按 SKU 取 seckill_goods.seckill_price 换价，秒杀需校验场次时段</li>
 *   <li>group：activityId = group_active.id，按 SKU 取 group_price 换价，校验活动有效期</li>
 *   <li>bargain：activityId = bargain_record.id（用户自己的砍价记录），取 current_price 换价，校验未过期</li>
 * </ul>
 * 取活动价替换当前价：discountDetail["activity"] = (当前价 - 活动价) × 数量，并把 item.currentPrice
 * 改为活动价，使后续满减/券门槛按"活动后小计"判定（与 FullReduce/Coupon 口径一致）。
 * <p>
 * 活动商品被换价后，MemberDiscountHandler 会跳过它（活动商品默认不叠加会员折扣）。
 */
@Component
public class ActivityPriceHandler implements PriceHandler {

    private final SeckillActiveService seckillActiveService;
    private final SeckillGoodsService seckillGoodsService;
    private final SeckillTimeService seckillTimeService;
    private final GroupActiveService groupActiveService;
    private final BargainActiveService bargainActiveService;
    private final BargainRecordService bargainRecordService;
    private final ObjectMapper objectMapper;

    public ActivityPriceHandler(SeckillActiveService seckillActiveService,
                                  SeckillGoodsService seckillGoodsService,
                                  SeckillTimeService seckillTimeService,
                                  GroupActiveService groupActiveService,
                                  BargainActiveService bargainActiveService,
                                  BargainRecordService bargainRecordService,
                                  ObjectMapper objectMapper) {
        this.seckillActiveService = seckillActiveService;
        this.seckillGoodsService = seckillGoodsService;
        this.seckillTimeService = seckillTimeService;
        this.groupActiveService = groupActiveService;
        this.bargainActiveService = bargainActiveService;
        this.bargainRecordService = bargainRecordService;
        this.objectMapper = objectMapper;
    }

    @Override
    public PriceHandlerType type() {
        return PriceHandlerType.ACTIVITY;
    }

    @Override
    public void handle(PriceContext ctx, PriceWorkingState state) {
        if (ctx.activityId() == null) {
            return;
        }
        switch (String.valueOf(ctx.activityType())) {
            case "seckill" -> handleSeckill(ctx, state);
            case "group" -> handleGroup(ctx, state);
            case "bargain" -> handleBargain(ctx, state);
            default -> { /* none 或未知：不生效 */ }
        }
    }

    private void handleSeckill(PriceContext ctx, PriceWorkingState state) {
        SeckillActive active = seckillActiveService.getByIdWithTenant(ctx.activityId());
        if (!"on".equals(active.getStatus())) {
            throw new BusinessException(ErrorCode.ACTIVITY_EXPIRED, "活动已结束");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(active.getStartDate()) || today.isAfter(active.getEndDate())) {
            throw new BusinessException(ErrorCode.ACTIVITY_EXPIRED, "活动未在有效期内");
        }

        List<Long> timeIds = parseIds(active.getTimeIds());
        if (!timeIds.isEmpty()) {
            List<SeckillTime> times = seckillTimeService.listByIds(timeIds);
            LocalTime now = LocalTime.now();
            boolean inSlot = times.stream().anyMatch(t ->
                    "on".equals(t.getStatus())
                            && !now.isBefore(t.getStartTime())
                            && !now.isAfter(t.getEndTime()));
            if (!inSlot) {
                throw new BusinessException(ErrorCode.ACTIVITY_EXPIRED, "秒杀未在抢购时段内");
            }
        }

        for (PriceWorkingState.WorkingItem item : state.items()) {
            SeckillGoods sg = seckillGoodsService.findByActiveAndSku(active.getId(), item.source().skuId());
            if (sg == null) {
                continue;
            }
            if (sg.getSeckillNum() != null && sg.getSeckillNum() > 0
                    && sg.getSold() != null && sg.getSold() >= sg.getSeckillNum()) {
                continue;
            }
            BigDecimal seckillPrice = sg.getSeckillPrice();
            if (seckillPrice == null || seckillPrice.compareTo(item.currentPrice()) >= 0) {
                continue;
            }
            BigDecimal discount = item.currentPrice().subtract(seckillPrice)
                    .multiply(BigDecimal.valueOf(item.source().quantity()));
            item.setCurrentPrice(seckillPrice);
            item.addDiscount("activity", discount);
        }
    }

    private void handleGroup(PriceContext ctx, PriceWorkingState state) {
        GroupActive active = groupActiveService.getByIdWithTenant(ctx.activityId());
        if (!"on".equals(active.getStatus())) {
            throw new BusinessException(ErrorCode.GROUP_EXPIRED, "拼团活动已结束");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(active.getStartTime()) || now.isAfter(active.getEndTime())) {
            throw new BusinessException(ErrorCode.GROUP_EXPIRED, "拼团活动未在有效期内");
        }
        Map<Long, BigDecimal> groupPrices = groupActiveService.parseGroupPrice(active);
        if (groupPrices.isEmpty()) {
            return;
        }
        for (PriceWorkingState.WorkingItem item : state.items()) {
            if (!active.getGoodsId().equals(item.source().goodsId())) {
                continue;
            }
            BigDecimal groupPrice = groupPrices.get(item.source().skuId());
            if (groupPrice == null || groupPrice.compareTo(item.currentPrice()) >= 0) {
                continue;
            }
            BigDecimal discount = item.currentPrice().subtract(groupPrice)
                    .multiply(BigDecimal.valueOf(item.source().quantity()));
            item.setCurrentPrice(groupPrice);
            item.addDiscount("activity", discount);
        }
    }

    private void handleBargain(PriceContext ctx, PriceWorkingState state) {
        BargainRecord record = bargainRecordService.getByIdWithTenant(ctx.activityId());
        // 仅本人砍价记录可用，且必须 ongoing/done 且未过期
        if (ctx.userId() != null && !ctx.userId().equals(record.getUserId())) {
            throw new BusinessException(ErrorCode.BARGAIN_NOT_FOUND, "砍价记录不属于当前用户");
        }
        String st = record.getStatus();
        if (!"ongoing".equals(st) && !"done".equals(st)) {
            throw new BusinessException(ErrorCode.BARGAIN_EXPIRED, "砍价已结束或已使用");
        }
        if (record.getExpireTime() != null && LocalDateTime.now().isAfter(record.getExpireTime())) {
            throw new BusinessException(ErrorCode.BARGAIN_EXPIRED, "砍价已过期");
        }
        BargainActive active = bargainActiveService.getByIdWithTenant(record.getActiveId());
        BigDecimal bargainPrice = record.getCurrentPrice();
        for (PriceWorkingState.WorkingItem item : state.items()) {
            if (!active.getGoodsId().equals(item.source().goodsId())) {
                continue;
            }
            if (bargainPrice == null || bargainPrice.compareTo(item.currentPrice()) >= 0) {
                continue;
            }
            BigDecimal discount = item.currentPrice().subtract(bargainPrice)
                    .multiply(BigDecimal.valueOf(item.source().quantity()));
            item.setCurrentPrice(bargainPrice);
            item.addDiscount("activity", discount);
        }
    }

    private List<Long> parseIds(String json) {
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
}
