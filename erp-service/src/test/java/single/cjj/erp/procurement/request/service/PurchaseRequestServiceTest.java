package single.cjj.erp.procurement.request.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.service.BusinessEventOutboxService;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.ApprovalResultRequest;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.CreateRequest;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.Detail;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts.EntryRequest;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntity;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntryEntity;
import single.cjj.erp.procurement.request.mapper.PurchaseRequestEntryMapper;
import single.cjj.erp.procurement.request.mapper.PurchaseRequestMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestServiceTest {

    @Mock
    private PurchaseRequestMapper requestMapper;
    @Mock
    private PurchaseRequestEntryMapper entryMapper;
    @Mock
    private BusinessEventOutboxService outboxService;

    @Test
    void shouldCreateAndCalculateEstimatedAmount() {
        AtomicReference<PurchaseRequestEntity> saved = new AtomicReference<>();
        List<PurchaseRequestEntryEntity> entries = new ArrayList<>();
        when(requestMapper.selectCount(any())).thenReturn(0L);
        when(requestMapper.insert(any())).thenAnswer(invocation -> {
            saved.set(invocation.getArgument(0));
            return 1;
        });
        when(entryMapper.insert(any())).thenAnswer(invocation -> {
            entries.add(invocation.getArgument(0));
            return 1;
        });
        when(requestMapper.selectOne(any())).thenAnswer(invocation -> saved.get());
        when(entryMapper.selectList(any())).thenAnswer(invocation -> entries);

        PurchaseRequestService service =
                new PurchaseRequestService(requestMapper, entryMapper, outboxService);

        Detail detail = service.create(new CreateRequest(
                "tenant-a", 100L, "PR-TEST-001", LocalDate.of(2026, 8, 27),
                88L, 200L, "PROJECT", "项目采购需求", "CNY",
                new BigDecimal("2000"), LocalDate.of(2026, 9, 10),
                300L, 400L, "PROJECT", "P-1", "PROJECT-001",
                List.of(
                        new EntryRequest(
                                1L, "M001", "物料1", null, 1L,
                                new BigDecimal("10"), new BigDecimal("100"),
                                null, null, null),
                        new EntryRequest(
                                2L, "M002", "物料2", null, 1L,
                                new BigDecimal("2"), new BigDecimal("50"),
                                null, null, null)
                )
        ), 88L);

        assertEquals("DRAFT", detail.request().getFstatus());
        assertEquals("DRAFT", detail.request().getFapprovalStatus());
        assertEquals("NONE", detail.request().getFexecutionStatus());
        assertEquals(new BigDecimal("12"), detail.request().getFtotalQuantity());
        assertEquals(new BigDecimal("1100.00"), detail.request().getFestimatedAmount());
        assertEquals(2, detail.entries().size());
    }

    @Test
    void approvedRequestShouldEmitBusinessEvent() {
        PurchaseRequestEntity request = request("SUBMITTED", "DRAFT", "NONE");
        PurchaseRequestEntryEntity entry = new PurchaseRequestEntryEntity();
        entry.setFid(11L);
        entry.setFpurchaseRequestId(1L);
        entry.setFmaterialId(2L);
        entry.setFmaterialCode("M001");
        entry.setFmaterialName("物料1");
        entry.setFquantity(new BigDecimal("5"));
        entry.setFestimatedUnitPrice(new BigDecimal("20"));
        entry.setFestimatedAmount(new BigDecimal("100"));

        when(requestMapper.selectByIdForUpdate(1L, "tenant-a")).thenReturn(request);
        when(requestMapper.updateById(any())).thenReturn(1);
        when(entryMapper.selectList(any())).thenReturn(List.of(entry));

        PurchaseRequestService service =
                new PurchaseRequestService(requestMapper, entryMapper, outboxService);

        Detail detail = service.applyApprovalResult(
                1L,
                "tenant-a",
                new ApprovalResultRequest("APPROVED", "WF-001", 99L, null));

        assertEquals("EFFECTIVE", detail.request().getFstatus());
        assertEquals("APPROVED", detail.request().getFapprovalStatus());
        assertEquals("WF-001", detail.request().getFworkflowInstanceId());

        verify(outboxService).append(
                eq("tenant-a"), eq(100L), eq("PURCHASE_REQUEST_APPROVED"),
                eq("PURCHASE_REQUEST"), eq(1L), any(),
                eq("ERP_PURCHASE_REQUEST"), eq("PR-001"),
                eq(LocalDate.of(2026, 8, 27)), eq(99L), any());
    }

    @Test
    void requestInExecutionCannotBeCancelled() {
        PurchaseRequestEntity request = request("APPROVED", "EFFECTIVE", "SOURCING");
        when(requestMapper.selectByIdForUpdate(1L, "tenant-a")).thenReturn(request);

        PurchaseRequestService service =
                new PurchaseRequestService(requestMapper, entryMapper, outboxService);

        assertThrows(BizException.class, () -> service.cancel(1L, "tenant-a", 99L));
    }

    private PurchaseRequestEntity request(
            String approvalStatus,
            String status,
            String executionStatus
    ) {
        PurchaseRequestEntity request = new PurchaseRequestEntity();
        request.setFid(1L);
        request.setFtenantId("tenant-a");
        request.setForgId(100L);
        request.setFnumber("PR-001");
        request.setFdate(LocalDate.of(2026, 8, 27));
        request.setFrequesterId(88L);
        request.setFrequestType("PROJECT");
        request.setFcurrencyCode("CNY");
        request.setFbudgetAmount(new BigDecimal("1000"));
        request.setFestimatedAmount(new BigDecimal("100"));
        request.setFtotalQuantity(new BigDecimal("5"));
        request.setFapprovalStatus(approvalStatus);
        request.setFstatus(status);
        request.setFexecutionStatus(executionStatus);
        request.setFversion(0);
        return request;
    }
}
