package com.shopplatform.domain.member.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.member.entity.RechargePlan;
import com.shopplatform.domain.member.mapper.RechargePlanMapper;
import com.shopplatform.domain.member.service.RechargePlanService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RechargePlanServiceImpl extends ServiceImpl<RechargePlanMapper, RechargePlan> implements RechargePlanService {

    @Override
    public List<RechargePlan> listShown() {
        return this.list(Wrappers.<RechargePlan>lambdaQuery()
                .eq(RechargePlan::getIsShow, 1)
                .orderByAsc(RechargePlan::getSort));
    }
}
