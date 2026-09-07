package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.shop.entity.PackageTpl;
import com.shopplatform.domain.shop.mapper.PackageTplMapper;
import com.shopplatform.domain.shop.service.PackageTplService;
import org.springframework.stereotype.Service;

@Service
public class PackageTplServiceImpl extends ServiceImpl<PackageTplMapper, PackageTpl> implements PackageTplService {
}
