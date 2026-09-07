package com.shopplatform.storeapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.marketing.entity.GroupActive;
import com.shopplatform.domain.marketing.service.GroupActiveService;
import com.shopplatform.storeapi.dto.SaveGroupActiveRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 商户后台拼团活动配置 CRUD。对应原型 docs/prototype/store/marketing-group.html。
 * group_price 存 JSON：{"skuId": price}。
 */
@RestController
@RequestMapping("/store/group")
public class StoreGroupController {

    private final GroupActiveService groupActiveService;
    private final ObjectMapper objectMapper;

    public StoreGroupController(GroupActiveService groupActiveService, ObjectMapper objectMapper) {
        this.groupActiveService = groupActiveService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/actives")
    public Result<List<GroupActive>> list() {
        return Result.ok(groupActiveService.listAll());
    }

    @PostMapping("/actives")
    public Result<GroupActive> create(@Valid @RequestBody SaveGroupActiveRequest req) {
        GroupActive a = new GroupActive();
        apply(req, a);
        groupActiveService.save(a);
        return Result.ok(a);
    }

    @PutMapping("/actives/{id}")
    public Result<GroupActive> update(@PathVariable Long id, @Valid @RequestBody SaveGroupActiveRequest req) {
        GroupActive a = groupActiveService.getByIdWithTenant(id);
        apply(req, a);
        groupActiveService.updateById(a);
        return Result.ok(a);
    }

    @DeleteMapping("/actives/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        groupActiveService.getByIdWithTenant(id);
        groupActiveService.removeById(id);
        return Result.ok();
    }

    private void apply(SaveGroupActiveRequest req, GroupActive a) {
        a.setGoodsId(req.goodsId());
        a.setGroupNum(req.groupNum());
        a.setGroupPrice(toJson(req.groupPrice()));
        a.setValidHours(req.validHours());
        a.setIsMock(req.isMock() == null ? 0 : req.isMock());
        a.setStartTime(req.startTime());
        a.setEndTime(req.endTime());
        a.setStatus(req.status() == null ? "on" : req.status());
    }

    private String toJson(Map<Long, java.math.BigDecimal> prices) {
        if (prices == null || prices.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "拼团价不能为空");
        }
        // key 序列化为字符串形式的 skuId
        TreeMap<String, java.math.BigDecimal> ordered = new TreeMap<>();
        prices.forEach((k, v) -> ordered.put(String.valueOf(k), v));
        try {
            return objectMapper.writeValueAsString(ordered);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "序列化拼团价失败");
        }
    }
}
