package com.shopplatform.domain.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shopplatform.domain.member.entity.UserBalanceLog;
import com.shopplatform.framework.mybatis.TenantSafeService;

public interface UserBalanceLogService extends TenantSafeService<UserBalanceLog> {

    IPage<UserBalanceLog> pageByUser(Long userId, long pageNum, long pageSize);
}
