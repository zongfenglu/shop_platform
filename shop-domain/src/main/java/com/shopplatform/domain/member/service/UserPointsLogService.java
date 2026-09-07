package com.shopplatform.domain.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shopplatform.domain.member.entity.UserPointsLog;
import com.shopplatform.framework.mybatis.TenantSafeService;

public interface UserPointsLogService extends TenantSafeService<UserPointsLog> {

    IPage<UserPointsLog> pageByUser(Long userId, long pageNum, long pageSize);
}
