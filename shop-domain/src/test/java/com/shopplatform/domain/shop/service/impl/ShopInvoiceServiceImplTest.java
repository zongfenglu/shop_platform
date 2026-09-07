package com.shopplatform.domain.shop.service.impl;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.shop.entity.ShopInvoice;
import com.shopplatform.domain.shop.entity.ShopOrder;
import com.shopplatform.domain.shop.service.ShopOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ShopInvoiceServiceImplTest {

    private ShopOrderService shopOrderService;
    private ShopInvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        shopOrderService = mock(ShopOrderService.class);
        service = spy(new ShopInvoiceServiceImpl(shopOrderService));
        doReturn(true).when(service).save(any());
        doReturn(true).when(service).updateById(any());
    }

    @Test
    void apply_paidOrder_createsApplying() {
        doReturn(paidOrder()).when(shopOrderService).getOne(any());
        doReturn(null).when(service).getOne(any());

        ShopInvoice row = service.apply(1001L, 9L, "某某公司", "91110000MA000");
        assertEquals("applying", row.getStatus());
        assertEquals("某某公司", row.getTitle());
        assertEquals(new BigDecimal("2680.00"), row.getAmount());
        verify(service).save(any(ShopInvoice.class));
    }

    @Test
    void apply_unpaid_throws20022() {
        ShopOrder unpaid = paidOrder();
        unpaid.setPayStatus("pending");
        doReturn(unpaid).when(shopOrderService).getOne(any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(1001L, 9L, "某某公司", "91110000MA000"));
        assertEquals(ErrorCode.INVOICE_ORDER_NOT_PAID.getCode(), ex.getCode());
    }

    @Test
    void apply_duplicateApplying_throws20023() {
        doReturn(paidOrder()).when(shopOrderService).getOne(any());
        ShopInvoice existing = new ShopInvoice();
        existing.setStatus("applying");
        doReturn(existing).when(service).getOne(any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.apply(1001L, 9L, "某某公司", "91110000MA000"));
        assertEquals(ErrorCode.INVOICE_ALREADY_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void apply_rejected_reusesRow() {
        doReturn(paidOrder()).when(shopOrderService).getOne(any());
        ShopInvoice existing = new ShopInvoice();
        existing.setId(3L);
        existing.setStatus("rejected");
        existing.setInvoiceNo("OLD");
        doReturn(existing).when(service).getOne(any());

        ShopInvoice row = service.apply(1001L, 9L, "新抬头", "91110000MA001");
        assertEquals("applying", row.getStatus());
        assertEquals("新抬头", row.getTitle());
        assertNull(row.getInvoiceNo());
        verify(service).updateById(existing);
    }

    @Test
    void issue_applying_assignsInvoiceNo() {
        ShopInvoice row = new ShopInvoice();
        row.setId(8L);
        row.setStatus("applying");
        doReturn(row).when(service).getOne(any());

        ShopInvoice issued = service.issue(8L, null);
        assertEquals("issued", issued.getStatus());
        assertTrue(issued.getInvoiceNo().startsWith("INV"));
        verify(service).updateById(row);
    }

    @Test
    void issue_notApplying_throws20024() {
        ShopInvoice row = new ShopInvoice();
        row.setId(8L);
        row.setStatus("issued");
        doReturn(row).when(service).getOne(any());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.issue(8L, "INV1"));
        assertEquals(ErrorCode.INVOICE_STATUS_INVALID.getCode(), ex.getCode());
    }

    private static ShopOrder paidOrder() {
        ShopOrder order = new ShopOrder();
        order.setId(9L);
        order.setShopId(1001L);
        order.setPayStatus("paid");
        order.setAmount(new BigDecimal("2680.00"));
        return order;
    }
}
