package com.shopplatform.domain.member.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.member.entity.UserBalanceLog;
import com.shopplatform.domain.member.mapper.UserBalanceLogMapper;
import com.shopplatform.domain.member.service.UserBalanceLogService;
import org.springframework.stereotype.Service;

@Service
public class UserBalanceLogServiceImpl extends ServiceImpl<UserBalanceLogMapper, UserBalanceLog> implements UserBalanceLogService {

    @Override
    public IPage<UserBalanceLog> pageByUser(Long userId, long pageNum, long pageSize) {
        return this.page(new Page<>(pageNum, pageSize),
                Wrappers.<UserBalanceLog>lambdaQuery()
                        .eq(UserBalanceLog::getUserId, userId)
                        .orderByDesc(UserBalanceLog::getCreateTime));
    }
}
