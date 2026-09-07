package com.shopplatform.domain.shop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.shop.entity.StoreRole;
import com.shopplatform.domain.shop.mapper.StoreRoleMapper;
import com.shopplatform.domain.shop.service.StoreRoleService;
import org.springframework.stereotype.Service;

@Service
public class StoreRoleServiceImpl extends ServiceImpl<StoreRoleMapper, StoreRole> implements StoreRoleService {
}
