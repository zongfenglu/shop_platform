package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.shop.entity.ShopDomain;
import com.shopplatform.domain.shop.mapper.ShopDomainMapper;
import com.shopplatform.domain.shop.service.ShopDomainService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ShopDomainServiceImpl extends ServiceImpl<ShopDomainMapper, ShopDomain> implements ShopDomainService {

    @Override
    public ShopDomain findByDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            return null;
        }
        return this.getOne(Wrappers.<ShopDomain>lambdaQuery()
                .eq(ShopDomain::getDomain, domain.trim().toLowerCase())
                .eq(ShopDomain::getVerifyStatus, "verified"));
    }

    @Override
    public ShopDomain findAnyByDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            return null;
        }
        return this.getOne(Wrappers.<ShopDomain>lambdaQuery()
                .eq(ShopDomain::getDomain, domain.trim().toLowerCase()));
    }

    @Override
    public List<ShopDomain> listByShopId(Long shopId) {
        return this.list(Wrappers.<ShopDomain>lambdaQuery()
                .eq(ShopDomain::getShopId, shopId)
                .orderByDesc(ShopDomain::getCreateTime));
    }
}
