package com.shopplatform.domain.pay.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.pay.entity.PayNotifyLog;
import com.shopplatform.domain.pay.mapper.PayNotifyLogMapper;
import com.shopplatform.domain.pay.service.PayNotifyLogService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class PayNotifyLogServiceImpl extends ServiceImpl<PayNotifyLogMapper, PayNotifyLog>
        implements PayNotifyLogService {

    @Override
    public boolean tryMarkProcessed(String channel, String transactionId, String outTradeNo) {
        PayNotifyLog log = new PayNotifyLog();
        log.setChannel(channel);
        log.setTransactionId(transactionId);
        log.setOutTradeNo(outTradeNo);
        log.setProcessResult("success");
        try {
            return this.save(log);
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
