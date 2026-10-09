package single.cjj.erp.procurement.order.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderCreateRequest;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderDetail;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderEntryRequest;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderMapper orderMapper;
    @Mock
    private PurchaseOrderEntryMapper entryMapper;
    @Mock
    private PurchaseOrderContractConversionService contractConversionService;

    @Test
    void shouldCalculateTotalsOnCreate() {
        AtomicReference<PurchaseOrderEntity> savedOrder = new AtomicReference<>();
        List<PurchaseOrderEntryEntity> savedEntries = new ArrayList<>();
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.insert(any())).thenAnswer(invocation -> {
            PurchaseOrderEntity order = invocation.getArgument(0);
            savedOrder.set(order);
            return 1;
        });
        when(entryMapper.insert(any())).thenAnswer(invocation -> {
            savedEntries.add(invocation.getArgument(0));
            return 1;
        });
        when(orderMapper.selectOne(any())).thenAnswer(invocation -> savedOrder.get());
        when(entryMapper.selectList(any())).thenAnswer(invocation -> savedEntries);

        PurchaseOrderService service = new PurchaseOrderService(orderMapper, entryMapper, contractConversionService);
        PurchaseOrderDetail detail = service.create(new PurchaseOrderCreateRequest(
                "tenant-a",
                100L,
                "PO-TEST-001",
                LocalDate.of(2026, 8, 26),
                9001L,
                "S001",
                "供应商A",
                null,
                null,
                "CNY",
                "NET30",
                LocalDate.of(2026, 9, 10),
                List.of(
                        new PurchaseOrderEntryRequest(
                                1L, "M001", "物料1", "SPEC-A", 1L,
                                new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("0.13"),
                                null, 200L, 300L
                        ),
                        new PurchaseOrderEntryRequest(
                                2L, "M002", "物料2", null, 1L,
                                new BigDecimal("2"), new BigDecimal("50"), BigDecimal.ZERO,
                                null, null, null
                        )
                )
        ), 88L);

        assertEquals("DRAFT", detail.order().getFstatus());
        assertEquals("DRAFT", detail.order().getFapprovalStatus());
        assertEquals(new BigDecimal("12"), detail.order().getFtotalQuantity());
        assertEquals(new BigDecimal("1100.00"), detail.order().getFnetAmount());
        assertEquals(new BigDecimal("130.00"), detail.order().getFtaxAmount());
        assertEquals(new BigDecimal("1230.00"), detail.order().getFgrossAmount());
        assertEquals(2, detail.entries().size());
        assertEquals(BigDecimal.ZERO, detail.entries().get(0).getFreceivedQuantity());
    }

    @Test
    void shouldSubmitAuditAndRejectDirectCancelAfterReceipt() {
        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setFid(1L);
        order.setFtenantId("tenant-a");
        order.setForgId(100L);
        order.setFstatus("DRAFT");
        order.setFapprovalStatus("DRAFT");
        order.setFreceiptStatus("NONE");
        order.setFversion(0);

        PurchaseOrderEntryEntity entry = new PurchaseOrderEntryEntity();
        entry.setFpurchaseOrderId(1L);
        entry.setFlineNo(1);

        when(orderMapper.selectOne(any())).thenReturn(order);
        when(entryMapper.selectList(any())).thenReturn(List.of(entry));
        when(orderMapper.updateById(any())).thenReturn(1);

        PurchaseOrderService service = new PurchaseOrderService(orderMapper, entryMapper, contractConversionService);
        service.submit(1L, "tenant-a", 88L);
        assertEquals("SUBMITTED", order.getFapprovalStatus());

        service.audit(1L, "tenant-a", 99L);
        assertEquals("AUDITED", order.getFapprovalStatus());
        assertEquals("EFFECTIVE", order.getFstatus());

        order.setFreceiptStatus("PARTIAL");
        assertThrows(BizException.class, () -> service.cancel(1L, "tenant-a", 99L));
    }
}