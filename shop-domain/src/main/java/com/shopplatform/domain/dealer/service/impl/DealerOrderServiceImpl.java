package com.shopplatform.domain.dealer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.dealer.entity.DealerOrder;
import com.shopplatform.domain.dealer.entity.DealerSetting;
import com.shopplatform.domain.dealer.entity.DealerUser;
import com.shopplatform.domain.dealer.mapper.DealerOrderMapper;
import com.shopplatform.domain.dealer.service.DealerOrderService;
import com.shopplatform.domain.dealer.service.DealerSettingService;
import com.shopplatform.domain.dealer.service.DealerUserService;
import com.shopplatform.domain.goods.entity.Goods;
import com.shopplatform.domain.goods.service.GoodsService;
import com.shopplatform.domain.order.entity.OrderGoods;
import com.shopplatform.domain.order.service.OrderGoodsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DealerOrderServiceImpl extends ServiceImpl<DealerOrderMapper, DealerOrder> implements DealerOrderService {

    private static final Logger log = LoggerFactory.getLogger(DealerOrderServiceImpl.class);

    private final DealerSettingService dealerSettingService;
    private final DealerUserService dealerUserService;
    private final OrderGoodsService orderGoodsService;
    private final GoodsService goodsService;
    private final ObjectMapper objectMapper;

    public DealerOrderServiceImpl(DealerSettingService dealerSettingService, DealerUserService dealerUserService,
                                  OrderGoodsService orderGoodsService, GoodsService goodsService,
                                  ObjectMapper objectMapper) {
        this.dealerSettingService = dealerSettingService;
        this.dealerUserService = dealerUserService;
        this.orderGoodsService = orderGoodsService;
        this.goodsService = goodsService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void createPending(Long orderId, Long userId, BigDecimal orderTotal) {
        DealerSetting setting = dealerSettingService.getOrCreate();
        if (setting.getIsEnable() == null || setting.getIsEnable() == 0) {
            return; // 分销未开启，不生成佣金
        }

        // 查找该用户的上级分销商（通过 Member 的 inviter/dealer 推荐关系）
        // 当前简化：通过 dealer_user 中的 parent_id 反向查
        // 实际场景中，需要在用户下单时传入推荐人 dealerId
        // 这里先查该用户自己是否是分销商的下级（通过 Member 或其他方式）
        // 简化实现：查找该用户是否关联了某个分销商（实际应由推荐链接参数决定）
        DealerUser dealer = dealerUserService.getMyDealer(userId);
        if (dealer == null || !"active".equals(dealer.getStatus())) {
            // 该用户不是分销商，也不在分销链中，跳过
            log.debug("User {} is not a dealer, skip commission", userId);
            return;
        }
        // 佣金归属：有上级给上级，没上级给自己（自购返佣）
        DealerUser beneficiary = dealer;
        if (dealer.getParentId() != null) {
            DealerUser parent = dealerUserService.getByIdWithTenant(dealer.getParentId());
            if (parent != null && "active".equals(parent.getStatus())) {
                beneficiary = parent;
            }
        }

        BigDecimal amount = "goods".equals(setting.getCommissionType())
                ? calcGoodsCommission(orderId, orderTotal, setting)
                : calcOrderCommission(orderTotal, setting.getCommissionRate());
        if (amount.signum() <= 0) {
            return; // 全部商品佣金比例为 0 时不落记录，避免一堆 0 元佣金单
        }

        DealerOrder dOrder = new DealerOrder();
        dOrder.setOrderId(orderId);
        dOrder.setDealerUserId(beneficiary.getId());
        dOrder.setOrderTotal(orderTotal);
        dOrder.setCommissionRate(setting.getCommissionRate());
        dOrder.setCommissionAmount(amount);
        dOrder.setStatus("pending");
        save(dOrder);
    }

    /** 整单佣金：订单实付 × 店铺统一比例 */
    private BigDecimal calcOrderCommission(BigDecimal orderTotal, BigDecimal ratePercent) {
        return orderTotal.multiply(toRatio(ratePercent)).setScale(2, RoundingMode.HALF_DOWN);
    }

    /**
     * 按商品佣金：逐订单行计佣求和。每行的基数是"行实付"（goodsPrice×totalNum − 本行优惠分摊，
     * 与 RefundCalculator 的退款口径一致），比例取 goods.commission_rate，未单独设置的商品
     * 回退店铺默认比例。运费不参与计佣（订单行基数天然不含运费；整单模式的基数 payPrice 含运费，
     * 这是两种模式的口径差异之一，商户文档需说明）。
     */
    private BigDecimal calcGoodsCommission(Long orderId, BigDecimal orderTotal, DealerSetting setting) {
        List<OrderGoods> lines = orderGoodsService.listByOrderId(orderId);
        if (lines.isEmpty()) {
            // 查不到订单行时降级为整单口径，保证佣金不因数据异常而漏记
            log.warn("No order_goods rows for order {}, fall back to order-level commission", orderId);
            return calcOrderCommission(orderTotal, setting.getCommissionRate());
        }
        Set<Long> goodsIds = lines.stream().map(OrderGoods::getGoodsId).collect(Collectors.toSet());
        Map<Long, BigDecimal> rateByGoods = goodsService.listByIds(goodsIds).stream()
                .filter(g -> g.getCommissionRate() != null)
                .collect(Collectors.toMap(Goods::getId, Goods::getCommissionRate));

        BigDecimal total = BigDecimal.ZERO;
        for (OrderGoods line : lines) {
            BigDecimal ratePercent = rateByGoods.getOrDefault(line.getGoodsId(), setting.getCommissionRate());
            if (ratePercent == null || ratePercent.signum() <= 0) {
                continue;
            }
            BigDecimal linePaid = line.getGoodsPrice()
                    .multiply(BigDecimal.valueOf(line.getTotalNum()))
                    .subtract(sumDiscount(line.getDiscountDetail()));
            if (linePaid.signum() <= 0) {
                continue;
            }
            total = total.add(linePaid.multiply(toRatio(ratePercent)).setScale(2, RoundingMode.HALF_DOWN));
        }
        return total;
    }

    private BigDecimal sumDiscount(String discountDetailJson) {
        if (discountDetailJson == null || discountDetailJson.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            Map<String, BigDecimal> detail = objectMapper.readValue(discountDetailJson, new TypeReference<Map<String, BigDecimal>>() {
            });
            return detail.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal toRatio(BigDecimal ratePercent) {
        return ratePercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    @Override
    public List<DealerOrder> listByDealer(Long dealerUserId, String status) {
        LambdaQueryWrapper<DealerOrder> wrapper = new LambdaQueryWrapper<DealerOrder>()
                .eq(DealerOrder::getDealerUserId, dealerUserId)
                .orderByDesc(DealerOrder::getId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DealerOrder::getStatus, status);
        }
        return list(wrapper);
    }

    @Override
    public List<DealerOrder> listByShop(String status) {
        LambdaQueryWrapper<DealerOrder> wrapper = new LambdaQueryWrapper<DealerOrder>()
                .orderByDesc(DealerOrder::getId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DealerOrder::getStatus, status);
        }
        return list(wrapper);
    }
}
