package com.shopplatform.domain.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shopplatform.domain.shop.entity.ShopInvoice;

import java.util.List;

public interface ShopInvoiceService extends IService<ShopInvoice> {

    ShopInvoice apply(Long shopId, Long shopOrderId, String title, String taxNo);

    ShopInvoice issue(Long id, String invoiceNo);

    ShopInvoice reject(Long id, String reason);

    List<ShopInvoice> listByShop(Long shopId);
}
