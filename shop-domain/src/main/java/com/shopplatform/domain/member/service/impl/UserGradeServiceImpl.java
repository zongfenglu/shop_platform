package com.shopplatform.domain.member.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.member.entity.UserGrade;
import com.shopplatform.domain.member.mapper.UserGradeMapper;
import com.shopplatform.domain.member.service.UserGradeService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserGradeServiceImpl extends ServiceImpl<UserGradeMapper, UserGrade> implements UserGradeService {

    @Override
    public void seedDefaults() {
        // 幂等：已有等级则不重复灌入，避免建店重试产生重复等级
        if (this.count() > 0) {
            return;
        }
        UserGrade g1 = grade("普通会员", 0, 0, new BigDecimal("1.00"), "默认等级");
        UserGrade g2 = grade("银卡会员", 1, 1000, new BigDecimal("0.98"), "消费满 1000 元");
        UserGrade g3 = grade("金卡会员", 2, 5000, new BigDecimal("0.95"), "消费满 5000 元");
        UserGrade g4 = grade("钻石会员", 3, 20000, new BigDecimal("0.90"), "消费满 20000 元");
        this.saveBatch(List.of(g1, g2, g3, g4));
    }

    @Override
    public List<UserGrade> listAllOrdered() {
        return this.list(Wrappers.<UserGrade>lambdaQuery().orderByAsc(UserGrade::getWeight));
    }

    private UserGrade grade(String name, int weight, int growth, BigDecimal ratio, String remark) {
        UserGrade g = new UserGrade();
        g.setName(name);
        g.setWeight(weight);
        g.setGrowthValue(growth);
        g.setDiscountRatio(ratio);
        g.setRemark(remark);
        return g;
    }
}
