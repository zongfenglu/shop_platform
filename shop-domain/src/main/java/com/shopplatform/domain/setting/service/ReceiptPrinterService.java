package com.shopplatform.domain.setting.service;

import com.shopplatform.domain.setting.entity.ReceiptPrinter;
import com.shopplatform.framework.mybatis.TenantSafeService;

import java.util.List;

public interface ReceiptPrinterService extends TenantSafeService<ReceiptPrinter> {
    List<ReceiptPrinter> listAll();
}
