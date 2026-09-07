package com.shopplatform.storeapi.controller;

import com.shopplatform.common.result.Result;
import com.shopplatform.domain.marketing.entity.SignConfig;
import com.shopplatform.domain.marketing.service.ContinuousRule;
import com.shopplatform.domain.marketing.service.SignConfigService;
import com.shopplatform.storeapi.dto.SaveSignConfigRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 签到配置管理。对应原型 store 营销中心-签到。
 * 每租户一条配置，GET 查 / POST 覆盖保存。
 */
@RestController
@RequestMapping("/store/sign-config")
public class StoreSignConfigController {

    private final SignConfigService signConfigService;

    public StoreSignConfigController(SignConfigService signConfigService) {
        this.signConfigService = signConfigService;
    }

    @GetMapping
    public Result<Map<String, Object>> get() {
        SignConfig config = signConfigService.getOrCreate();
        List<ContinuousRule> rules = ContinuousRule.parseList(config.getContinuousRules());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dailyPoints", config.getDailyPoints());
        data.put("continuousRules", rules);
        return Result.ok(data);
    }

    @PostMapping
    public Result<Void> save(@Valid @RequestBody SaveSignConfigRequest req) {
        List<ContinuousRule> rules = new ArrayList<>();
        if (req.continuousRules() != null) {
            for (SaveSignConfigRequest.ContinuousRuleItem item : req.continuousRules()) {
                rules.add(new ContinuousRule(item.days(), item.type(), item.value(), item.couponId()));
            }
        }
        signConfigService.saveConfig(req.dailyPoints(), rules);
        return Result.ok();
    }
}
