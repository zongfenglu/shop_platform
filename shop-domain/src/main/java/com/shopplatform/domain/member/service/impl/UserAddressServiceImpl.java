package com.shopplatform.domain.member.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.member.entity.UserAddress;
import com.shopplatform.domain.member.mapper.UserAddressMapper;
import com.shopplatform.domain.member.service.UserAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {

    @Override
    public List<UserAddress> listByUser(Long userId) {
        return this.list(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getCreateTime));
    }

    @Override
    public UserAddress getOwnAddress(Long userId, Long addressId) {
        UserAddress address = this.getOne(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId));
        if (address == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收货地址不存在");
        }
        return address;
    }

    @Override
    public UserAddress findDefault(Long userId) {
        List<UserAddress> list = listByUser(userId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @Transactional
    public UserAddress create(UserAddress address, Long userId) {
        address.setId(null);
        address.setUserId(userId);
        boolean first = this.count(Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getUserId, userId)) == 0;
        boolean asDefault = first || Boolean.TRUE.equals(address.getIsDefault());
        address.setIsDefault(asDefault);
        if (asDefault) {
            clearDefaultFlag(userId);
        }
        this.save(address);
        return address;
    }

    @Override
    @Transactional
    public void update(UserAddress address, Long userId) {
        UserAddress existing = getOwnAddress(userId, address.getId());
        if (Boolean.TRUE.equals(address.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
            clearDefaultFlag(userId);
        } else {
            // 不允许直接把唯一/当前的默认地址取消默认——要换默认地址走 setDefault，
            // 否则会出现"一个默认都没有"的状态，结算页就得额外处理这个空档。
            address.setIsDefault(existing.getIsDefault());
        }
        address.setUserId(userId);
        this.updateById(address);
    }

    @Override
    public void delete(Long userId, Long addressId) {
        this.remove(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId));
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long addressId) {
        UserAddress address = getOwnAddress(userId, addressId);
        clearDefaultFlag(userId);
        address.setIsDefault(true);
        this.updateById(address);
    }

    private void clearDefaultFlag(Long userId) {
        this.update(Wrappers.<UserAddress>lambdaUpdate()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, true)
                .set(UserAddress::getIsDefault, false));
    }
}
