package com.shopplatform.domain.setting.service;

import com.shopplatform.domain.setting.entity.SmsChannel;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface SmsChannelService extends TenantSafeService<SmsChannel> {
    List<SmsChannel> listAll();
}
