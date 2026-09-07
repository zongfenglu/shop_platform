package com.shopplatform.domain.member.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.member.entity.UserPointsLog;
import com.shopplatform.domain.member.mapper.UserPointsLogMapper;
import com.shopplatform.domain.member.service.UserPointsLogService;
import org.springframework.stereotype.Service;

@Service
public class UserPointsLogServiceImpl extends ServiceImpl<UserPointsLogMapper, UserPointsLog> implements UserPointsLogService {

    @Override
    public IPage<UserPointsLog> pageByUser(Long userId, long pageNum, long pageSize) {
        return this.page(new Page<>(pageNum, pageSize),
                Wrappers.<UserPointsLog>lambdaQuery()
                        .eq(UserPointsLog::getUserId, userId)
                        .orderByDesc(UserPointsLog::getCreateTime));
    }
}
