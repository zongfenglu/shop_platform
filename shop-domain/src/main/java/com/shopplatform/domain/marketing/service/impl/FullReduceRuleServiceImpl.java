package com.shopplatform.domain.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.marketing.entity.FullReduceRule;
import com.shopplatform.domain.marketing.mapper.FullReduceRuleMapper;
import com.shopplatform.domain.marketing.service.FullReduceRuleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FullReduceRuleServiceImpl extends ServiceImpl<FullReduceRuleMapper, FullReduceRule> implements FullReduceRuleService {

    @Override
    public List<FullReduceRule> listActive() {
        return list(new LambdaQueryWrapper<FullReduceRule>()
                .eq(FullReduceRule::getStatus, "on")
                .orderByDesc(FullReduceRule::getSort)
                .orderByDesc(FullReduceRule::getId));
    }
}
