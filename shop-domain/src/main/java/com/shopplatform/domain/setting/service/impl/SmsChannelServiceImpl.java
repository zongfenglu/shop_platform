package com.shopplatform.domain.setting.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.setting.entity.SmsChannel;
import com.shopplatform.domain.setting.mapper.SmsChannelMapper;
import com.shopplatform.domain.setting.service.SmsChannelService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmsChannelServiceImpl extends ServiceImpl<SmsChannelMapper, SmsChannel>
        implements SmsChannelService {
    @Override
    public List<SmsChannel> listAll() {
        return list(Wrappers.<SmsChannel>lambdaQuery()
                .orderByAsc(SmsChannel::getPriority).orderByDesc(SmsChannel::getCreateTime));
    }
}
