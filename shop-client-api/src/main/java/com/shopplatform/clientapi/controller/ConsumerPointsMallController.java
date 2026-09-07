package com.shopplatform.clientapi.controller;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.marketing.entity.ExchangeRecord;
import com.shopplatform.domain.marketing.entity.PointsGoods;
import com.shopplatform.domain.marketing.service.ExchangeRecordService;
import com.shopplatform.domain.marketing.service.PointsGoodsService;
import com.shopplatform.framework.security.LoginUserContext;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消费者积分商城。对应原型 h5/points-mall.html。
 * GET /goods 公开（未登录也可浏览），兑换需登录。
 */
@RestController
@RequestMapping("/api/points-mall")
public class ConsumerPointsMallController {

    private final PointsGoodsService pointsGoodsService;
    private final ExchangeRecordService exchangeRecordService;

    public ConsumerPointsMallController(PointsGoodsService pointsGoodsService,
                                         ExchangeRecordService exchangeRecordService) {
        this.pointsGoodsService = pointsGoodsService;
        this.exchangeRecordService = exchangeRecordService;
    }

    /** 在架兑换项列表（公开） */
    @GetMapping("/goods")
    public Result<List<Map<String, Object>>> listGoods() {
        List<PointsGoods> goods = pointsGoodsService.listOnSale();
        List<Map<String, Object>> result = goods.stream().map(g -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId().toString());
            m.put("name", g.getName());
            m.put("image", g.getImage());
            m.put("points", g.getPoints());
            m.put("cash", g.getCash());
            m.put("stock", g.getStock());
            m.put("goodsType", g.getCouponId() != null ? "coupon" : "goods");
            return m;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }

    /** 创建兑换单 */
    @PostMapping("/exchanges")
    public Result<Map<String, Object>> createExchange(@RequestBody Map<String, Object> body) {
        Long goodsId = parseLong(body.get("goodsId"));
        ExchangeRecord record = exchangeRecordService.create(requireLoginUserId(), goodsId);
        return Result.ok(toMap(record));
    }

    /** 支付兑换单 */
    @PostMapping("/exchanges/{id}/pay")
    public Result<Map<String, Object>> payExchange(@PathVariable Long id) {
        ExchangeRecord record = exchangeRecordService.pay(requireLoginUserId(), id);
        return Result.ok(toMap(record));
    }

    private Map<String, Object> toMap(ExchangeRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId().toString());
        m.put("pointsCost", r.getPointsCost());
        m.put("cashPrice", r.getCashPrice());
        m.put("payStatus", r.getPayStatus());
        m.put("status", r.getStatus());
        m.put("goodsType", r.getGoodsType());
        return m;
    }

    private Long parseLong(Object v) {
        if (v == null) throw new BusinessException(ErrorCode.PARAM_INVALID, "缺少 goodsId");
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); }
        catch (NumberFormatException e) { throw new BusinessException(ErrorCode.PARAM_INVALID, "goodsId 格式错误"); }
    }

    private Long requireLoginUserId() {
        LoginUserContext.LoginUser user = LoginUserContext.get();
        if (user == null || user.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return user.userId();
    }
}
