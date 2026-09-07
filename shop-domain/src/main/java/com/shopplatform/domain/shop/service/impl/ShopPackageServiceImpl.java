package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.shop.entity.ShopPackage;
import com.shopplatform.domain.shop.mapper.ShopPackageMapper;
import com.shopplatform.domain.shop.service.ShopPackageService;
import org.springframework.stereotype.Service;

@Service
public class ShopPackageServiceImpl extends ServiceImpl<ShopPackageMapper, ShopPackage> implements ShopPackageService {
}
