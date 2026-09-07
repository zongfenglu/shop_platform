package com.shopplatform.storeapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.common.result.Result;
import com.shopplatform.domain.marketing.entity.FullReduceRule;
import com.shopplatform.domain.marketing.service.FullReduceRuleService;
import com.shopplatform.storeapi.dto.SaveFullReduceRuleRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 满减/满件折规则管理。对应原型 store 营销中心-满减活动。 */
@RestController
@RequestMapping("/store/full-reduce-rules")
public class StoreFullReduceRuleController {

    private final FullReduceRuleService fullReduceRuleService;
    private final ObjectMapper objectMapper;

    public StoreFullReduceRuleController(FullReduceRuleService fullReduceRuleService, ObjectMapper objectMapper) {
        this.fullReduceRuleService = fullReduceRuleService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public Result<List<FullReduceRule>> list() {
        return Result.ok(fullReduceRuleService.list());
    }

    @PostMapping
    public Result<FullReduceRule> create(@Valid @RequestBody SaveFullReduceRuleRequest req) {
        FullReduceRule rule = new FullReduceRule();
        apply(req, rule);
        fullReduceRuleService.save(rule);
        return Result.ok(rule);
    }

    @PutMapping("/{id}")
    public Result<FullReduceRule> update(@PathVariable Long id, @Valid @RequestBody SaveFullReduceRuleRequest req) {
        FullReduceRule rule = fullReduceRuleService.getByIdWithTenant(id);
        apply(req, rule);
        fullReduceRuleService.updateById(rule);
        return Result.ok(rule);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fullReduceRuleService.getByIdWithTenant(id);
        fullReduceRuleService.removeById(id);
        return Result.ok();
    }

    private void apply(SaveFullReduceRuleRequest req, FullReduceRule rule) {
        rule.setName(req.name());
        rule.setType(req.type());
        rule.setRules(toJson(req.rules()));
        rule.setFreeExpress(req.freeExpress() == null ? 0 : req.freeExpress());
        rule.setStatus(req.status() == null ? "on" : req.status());
        rule.setSort(req.sort() == null ? 0 : req.sort());
    }

    private String toJson(List<SaveFullReduceRuleRequest.Tier> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "满减规则至少需要一档");
        }
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "序列化满减规则失败");
        }
    }
}
