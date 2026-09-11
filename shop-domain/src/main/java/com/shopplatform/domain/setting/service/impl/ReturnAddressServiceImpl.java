package com.shopplatform.domain.setting.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.setting.entity.ReturnAddress;
import com.shopplatform.domain.setting.mapper.ReturnAddressMapper;
import com.shopplatform.domain.setting.service.ReturnAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReturnAddressServiceImpl extends ServiceImpl<ReturnAddressMapper, ReturnAddress>
        implements ReturnAddressService {
    @Override
    public List<ReturnAddress> listAll() {
        return list(Wrappers.<ReturnAddress>lambdaQuery()
                .orderByDesc(ReturnAddress::getIsDefault)
                .orderByAsc(ReturnAddress::getSort)
                .orderByDesc(ReturnAddress::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAsDefault(ReturnAddress address) {
        if (Boolean.TRUE.equals(address.getIsDefault()) || count() == 0) {
            update(Wrappers.<ReturnAddress>lambdaUpdate().set(ReturnAddress::getIsDefault, false));
            address.setIsDefault(true);
        }
        saveOrUpdate(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        ReturnAddress target = getByIdWithTenant(id);
        update(Wrappers.<ReturnAddress>lambdaUpdate().set(ReturnAddress::getIsDefault, false));
        target.setIsDefault(true);
        target.setStatus("enabled");
        updateById(target);
    }
}
