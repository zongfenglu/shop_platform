package com.shopplatform.domain.setting.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.setting.entity.StoreOperationSetting;
import com.shopplatform.domain.setting.mapper.StoreOperationSettingMapper;
import com.shopplatform.domain.setting.service.StoreOperationSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreOperationSettingServiceImpl extends ServiceImpl<StoreOperationSettingMapper, StoreOperationSetting>
        implements StoreOperationSettingService {
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StoreOperationSetting getOrCreate() {
        StoreOperationSetting setting = getOne(null);
        if (setting != null) return setting;
        setting = new StoreOperationSetting();
        setting.setUploadProvider("local");
        setting.setImageMaxMb(5);
        setting.setVideoMaxMb(50);
        setting.setPrintEnabled(false);
        setting.setPrintOnPaid(true);
        setting.setPrintOnRefund(false);
        setting.setPrintCopies(1);
        setting.setSmsEnabled(false);
        save(setting);
        return setting;
    }
}
