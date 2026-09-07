package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.marketing.entity.SignConfig;
import com.shopplatform.domain.marketing.mapper.SignConfigMapper;
import com.shopplatform.domain.marketing.service.ContinuousRule;
import com.shopplatform.domain.marketing.service.SignConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SignConfigServiceImpl extends ServiceImpl<SignConfigMapper, SignConfig> implements SignConfigService {

    @Override
    public SignConfig getOrCreate() {
        SignConfig config = getOne(new LambdaQueryWrapper<>());
        if (config == null) {
            config = new SignConfig();
            config.setDailyPoints(2);
            config.setContinuousRules("[]");
            save(config);
        }
        return config;
    }

    @Override
    public void saveConfig(int dailyPoints, List<ContinuousRule> rules) {
        SignConfig config = getOne(new LambdaQueryWrapper<>());
        if (config == null) {
            config = new SignConfig();
        }
        config.setDailyPoints(dailyPoints);
        config.setContinuousRules(ContinuousRule.toJson(rules));
        saveOrUpdate(config);
    }
}
