package com.shopplatform.domain.setting.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.domain.setting.entity.ReceiptPrinter;
import com.shopplatform.domain.setting.mapper.ReceiptPrinterMapper;
import com.shopplatform.domain.setting.service.ReceiptPrinterService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReceiptPrinterServiceImpl extends ServiceImpl<ReceiptPrinterMapper, ReceiptPrinter>
        implements ReceiptPrinterService {
    @Override
    public List<ReceiptPrinter> listAll() {
        return list(Wrappers.<ReceiptPrinter>lambdaQuery()
                .orderByAsc(ReceiptPrinter::getSort).orderByDesc(ReceiptPrinter::getCreateTime));
    }
}
