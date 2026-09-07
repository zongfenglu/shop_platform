package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.marketing.entity.ExchangeRecord;
import com.shopplatform.domain.marketing.entity.PointsGoods;
import com.shopplatform.domain.marketing.mapper.ExchangeRecordMapper;
import com.shopplatform.domain.marketing.mapper.PointsGoodsMapper;
import com.shopplatform.domain.marketing.service.ExchangeRecordService;
import com.shopplatform.domain.marketing.service.PointsGoodsService;
import com.shopplatform.domain.marketing.service.UserCouponService;
import com.shopplatform.domain.member.entity.Member;
import com.shopplatform.domain.member.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ExchangeRecordServiceImpl extends ServiceImpl<ExchangeRecordMapper, ExchangeRecord>
        implements ExchangeRecordService {

    private final PointsGoodsService pointsGoodsService;
    private final PointsGoodsMapper pointsGoodsMapper;
    private final MemberService memberService;
    private final UserCouponService userCouponService;

    public ExchangeRecordServiceImpl(PointsGoodsService pointsGoodsService,
                                      PointsGoodsMapper pointsGoodsMapper,
                                      MemberService memberService,
                                      UserCouponService userCouponService) {
        this.pointsGoodsService = pointsGoodsService;
        this.pointsGoodsMapper = pointsGoodsMapper;
        this.memberService = memberService;
        this.userCouponService = userCouponService;
    }

    @Override
    public ExchangeRecord create(Long userId, Long pointsGoodsId) {
        PointsGoods goods = pointsGoodsService.getByIdWithTenant(pointsGoodsId);
        if (goods == null || !"on".equals(goods.getStatus())) {
            throw new BusinessException(ErrorCode.POINTS_GOODS_NOT_FOUND, "兑换商品不存在或已下架");
        }

        // 校验积分是否足够
        Member member = memberService.getByIdWithTenant(userId);
        if (member == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        int memberPoints = member.getPoints() == null ? 0 : member.getPoints();
        if (memberPoints < goods.getPoints()) {
            throw new BusinessException(ErrorCode.POINTS_INSUFFICIENT, "积分不足");
        }

        String goodsType = goods.getCouponId() != null ? "coupon" : "goods";

        ExchangeRecord record = new ExchangeRecord();
        record.setUserId(userId);
        record.setPointsGoodsId(pointsGoodsId);
        record.setGoodsType(goodsType);
        record.setCouponId(goods.getCouponId());
        record.setPointsCost(goods.getPoints());
        record.setCashPrice(goods.getCash() == null ? BigDecimal.ZERO : goods.getCash());
        record.setPayStatus("unpaid");
        record.setStatus("pending");
        record.setName(goods.getName());
        record.setImage(goods.getImage());
        save(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExchangeRecord pay(Long userId, Long exchangeId) {
        ExchangeRecord record = getByIdWithTenant(exchangeId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "兑换单不存在");
        }
        if (!"unpaid".equals(record.getPayStatus())) {
            throw new BusinessException(ErrorCode.EXCHANGE_STATUS_INVALID, "兑换单已支付");
        }

        // 1. 原子扣库存（stock=0 不限，跳过）
        int stockResult = pointsGoodsMapper.decrStock(record.getPointsGoodsId());
        // stock=0 时 decrStock 返回 0（不更新）；此时需再查一下确认是否不限量
        PointsGoods goods = pointsGoodsService.getByIdWithTenant(record.getPointsGoodsId());
        if (goods == null) {
            throw new BusinessException(ErrorCode.POINTS_GOODS_NOT_FOUND, "兑换商品已下架");
        }
        if (goods.getStock() != null && goods.getStock() > 0 && stockResult == 0) {
            throw new BusinessException(ErrorCode.EXCHANGE_STOCK_INSUFFICIENT, "兑换商品库存不足");
        }

        // 2. 原子扣积分
        memberService.adjustPoints(userId, -record.getPointsCost(), "mall", "积分商城兑换");

        // 3. 发券 or 待发货
        if ("coupon".equals(record.getGoodsType())) {
            if (record.getCouponId() != null) {
                var uc = userCouponService.issueForReward(userId, record.getCouponId());
                record.setUserCouponId(uc.getId());
            }
            record.setPayStatus("paid");
        } else {
            record.setPayStatus("paid");
            // goods 型保持 status=pending (待发货)
        }

        updateById(record);
        return record;
    }

    @Override
    public List<ExchangeRecord> listByShop(String status) {
        LambdaQueryWrapper<ExchangeRecord> wrapper = new LambdaQueryWrapper<ExchangeRecord>()
                .orderByDesc(ExchangeRecord::getId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ExchangeRecord::getStatus, status);
        }
        return list(wrapper);
    }
}
