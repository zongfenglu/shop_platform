package com.shopplatform.domain.setting.service;

import com.shopplatform.domain.setting.entity.ReturnAddress;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface ReturnAddressService extends TenantSafeService<ReturnAddress> {
    List<ReturnAddress> listAll();
    void saveAsDefault(ReturnAddress address);
    void setDefault(Long id);
}
