package com.shopplatform.domain.dealer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.dealer.entity.DealerSetting;
import com.shopplatform.domain.dealer.mapper.DealerSettingMapper;
import com.shopplatform.domain.dealer.service.DealerSettingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DealerSettingServiceImpl extends ServiceImpl<DealerSettingMapper, DealerSetting> implements DealerSettingService {

    @Override
    public DealerSetting getOrCreate() {
        DealerSetting setting = getOne(new LambdaQueryWrapper<>());
        if (setting == null) {
            setting = new DealerSetting();
            setting.setIsEnable(0);
            setting.setCommissionRate(new BigDecimal("10.00"));
            setting.setCommissionType("order");
            setting.setMinWithdraw(new BigDecimal("10.00"));
            setting.setAutoApprove(0);
            save(setting);
        }
        return setting;
    }

    @Override
    public void saveSetting(DealerSetting setting) {
        DealerSetting existing = getOne(new LambdaQueryWrapper<>());
        if (existing != null) {
            setting.setId(existing.getId());
        }
        saveOrUpdate(setting);
    }
}
