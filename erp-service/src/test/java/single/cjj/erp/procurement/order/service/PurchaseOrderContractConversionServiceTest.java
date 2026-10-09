package single.cjj.erp.procurement.order.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntity;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntryEntity;
import single.cjj.erp.procurement.contract.mapper.PurchaseContractEntryMapper;
import single.cjj.erp.procurement.contract.mapper.PurchaseContractMapper;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderFromContractCreateRequest;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderFromContractEntryRequest;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntity;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderEntryMapper;
import single.cjj.erp.procurement.order.mapper.PurchaseOrderMapper;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntity;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntryEntity;
import single.cjj.erp.procurement.request.mapper.PurchaseRequestEntryMapper;
import single.cjj.erp.procurement.request.mapper.PurchaseRequestMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderContractConversionServiceTest {

    @Mock PurchaseOrderMapper orderMapper;
    @Mock PurchaseOrderEntryMapper orderEntryMapper;
    @Mock PurchaseContractMapper contractMapper;
    @Mock PurchaseContractEntryMapper contractEntryMapper;
    @Mock PurchaseRequestMapper requestMapper;
    @Mock PurchaseRequestEntryMapper requestEntryMapper;

    @Test
    void shouldCreateDraftOrderFromEffectiveContractAndWriteBackQuantities() {
        PurchaseContractEntity contract = contract();
        PurchaseContractEntryEntity contractEntry = contractEntry();
        PurchaseRequestEntryEntity requestEntry = requestEntry();
        PurchaseRequestEntity request = request();

        when(contractMapper.selectByIdForUpdate(800L, "tenant-a")).thenReturn(contract);
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(contractEntryMapper.selectByIdForUpdate(801L, "tenant-a"))
                .thenReturn(contractEntry);
        when(requestEntryMapper.selectByIdForUpdate(11L, "tenant-a"))
                .thenReturn(requestEntry);
        when(contractEntryMapper.updateById(any())).thenReturn(1);
        when(requestEntryMapper.updateById(any())).thenReturn(1);
        when(orderMapper.insert(any())).thenReturn(1);
        when(orderEntryMapper.insert(any())).thenReturn(1);
        when(contractEntryMapper.selectList(any())).thenReturn(List.of(contractEntry));
        when(contractMapper.updateById(any())).thenReturn(1);
        when(requestMapper.selectByIdForUpdate(1L, "tenant-a")).thenReturn(request);
        when(requestEntryMapper.selectList(any())).thenReturn(List.of(requestEntry));
        when(requestMapper.updateById(any())).thenReturn(1);

        var detail = service().createFromContract(new PurchaseOrderFromContractCreateRequest(
                "tenant-a",
                800L,
                "PO-C-001",
                LocalDate.of(2026, 9, 5),
                List.of(new PurchaseOrderFromContractEntryRequest(
                        801L,
                        new BigDecimal("10"),
                        null
                ))
        ), 99L);

        assertEquals(800L, detail.order().getFcontractId());
        assertEquals(500L, detail.order().getFbusinessPartnerId());
        assertEquals("CNY", detail.order().getFcurrencyCode());
        assertEquals(new BigDecimal("1000.00"), detail.order().getFnetAmount());
        assertEquals(new BigDecimal("130.00"), detail.order().getFtaxAmount());
        assertEquals(new BigDecimal("1130.00"), detail.order().getFgrossAmount());
        assertEquals("DRAFT", detail.order().getFstatus());

        PurchaseOrderEntryEntity orderEntry = detail.entries().get(0);
        assertEquals(801L, orderEntry.getFcontractEntryId());
        assertEquals(301L, orderEntry.getFsourcingAwardEntryId());
        assertEquals(101L, orderEntry.getFrfqEntryId());
        assertEquals(1L, orderEntry.getFpurchaseRequestId());
        assertEquals(11L, orderEntry.getFpurchaseRequestEntryId());

        assertEquals(new BigDecimal("10"), contractEntry.getForderedQuantity());
        assertEquals(new BigDecimal("10"), requestEntry.getForderedQuantity());
        assertEquals("COMPLETE", contract.getFexecutionStatus());
        assertEquals("COMPLETE", request.getFexecutionStatus());
    }

    @Test
    void shouldRejectOrderQuantityBeyondContractRemainingQuantity() {
        PurchaseContractEntryEntity contractEntry = contractEntry();
        contractEntry.setForderedQuantity(new BigDecimal("8"));

        when(contractMapper.selectByIdForUpdate(800L, "tenant-a"))
                .thenReturn(contract());
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(contractEntryMapper.selectByIdForUpdate(801L, "tenant-a"))
                .thenReturn(contractEntry);

        assertThrows(BizException.class, () -> service().createFromContract(
                new PurchaseOrderFromContractCreateRequest(
                        "tenant-a",
                        800L,
                        "PO-C-002",
                        LocalDate.of(2026, 9, 5),
                        List.of(new PurchaseOrderFromContractEntryRequest(
                                801L,
                                new BigDecimal("3"),
                                null
                        ))
                ),
                99L
        ));
    }

    @Test
    void shouldRejectOrderOutsideContractValidity() {
        PurchaseContractEntity contract = contract();
        when(contractMapper.selectByIdForUpdate(800L, "tenant-a"))
                .thenReturn(contract);

        assertThrows(BizException.class, () -> service().createFromContract(
                new PurchaseOrderFromContractCreateRequest(
                        "tenant-a",
                        800L,
                        "PO-C-003",
                        LocalDate.of(2027, 1, 1),
                        List.of(new PurchaseOrderFromContractEntryRequest(
                                801L,
                                BigDecimal.ONE,
                                null
                        ))
                ),
                99L
        ));
    }

    @Test
    void releaseShouldRestoreContractAndRequestExecutionStatus() {
        PurchaseContractEntity contract = contract();
        contract.setFexecutionStatus("ORDERING");
        PurchaseContractEntryEntity contractEntry = contractEntry();
        contractEntry.setForderedQuantity(new BigDecimal("5"));
        PurchaseRequestEntryEntity requestEntry = requestEntry();
        requestEntry.setFsourcedQuantity(new BigDecimal("10"));
        requestEntry.setForderedQuantity(new BigDecimal("5"));
        PurchaseRequestEntity request = request();
        request.setFexecutionStatus("ORDERING");

        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setFid(1000L);
        order.setFtenantId("tenant-a");
        order.setFcontractId(800L);

        PurchaseOrderEntryEntity orderEntry = new PurchaseOrderEntryEntity();
        orderEntry.setFcontractEntryId(801L);
        orderEntry.setFpurchaseRequestEntryId(11L);
        orderEntry.setFquantity(new BigDecimal("5"));

        when(contractMapper.selectByIdForUpdate(800L, "tenant-a"))
                .thenReturn(contract);
        when(contractEntryMapper.selectByIdForUpdate(801L, "tenant-a"))
                .thenReturn(contractEntry);
        when(contractEntryMapper.updateById(any())).thenReturn(1);
        when(requestEntryMapper.selectByIdForUpdate(11L, "tenant-a"))
                .thenReturn(requestEntry);
        when(requestEntryMapper.updateById(any())).thenReturn(1);
        when(contractEntryMapper.selectList(any())).thenReturn(List.of(contractEntry));
        when(contractMapper.updateById(any())).thenReturn(1);
        when(requestMapper.selectByIdForUpdate(1L, "tenant-a")).thenReturn(request);
        when(requestEntryMapper.selectList(any())).thenReturn(List.of(requestEntry));
        when(requestMapper.updateById(any())).thenReturn(1);

        service().releaseOrderAllocation(order, List.of(orderEntry), 99L);

        assertEquals(BigDecimal.ZERO, contractEntry.getForderedQuantity());
        assertEquals(BigDecimal.ZERO, requestEntry.getForderedQuantity());
        assertEquals("NONE", contract.getFexecutionStatus());
        assertEquals("CONTRACTING", request.getFexecutionStatus());
    }

    private PurchaseOrderContractConversionService service() {
        return new PurchaseOrderContractConversionService(
                orderMapper,
                orderEntryMapper,
                contractMapper,
                contractEntryMapper,
                requestMapper,
                requestEntryMapper
        );
    }

    private PurchaseContractEntity contract() {
        PurchaseContractEntity value = new PurchaseContractEntity();
        value.setFid(800L);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFnumber("PC-001");
        value.setFbusinessPartnerId(500L);
        value.setFbusinessPartnerCode("SUP-A");
        value.setFbusinessPartnerName("供应商A");
        value.setFcurrencyCode("CNY");
        value.setFpaymentTermCode("NET30");
        value.setFstartDate(LocalDate.of(2026, 9, 1));
        value.setFendDate(LocalDate.of(2026, 12, 31));
        value.setFstatus("EFFECTIVE");
        value.setFapprovalStatus("APPROVED");
        value.setFexecutionStatus("NONE");
        value.setFversion(0);
        return value;
    }

    private PurchaseContractEntryEntity contractEntry() {
        PurchaseContractEntryEntity value = new PurchaseContractEntryEntity();
        value.setFid(801L);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFpurchaseContractId(800L);
        value.setFsourcingAwardEntryId(301L);
        value.setFrfqEntryId(101L);
        value.setFpurchaseRequestId(1L);
        value.setFpurchaseRequestEntryId(11L);
        value.setFmaterialId(100L);
        value.setFmaterialCode("M001");
        value.setFmaterialName("物料1");
        value.setFunitId(1L);
        value.setFquantity(new BigDecimal("10"));
        value.setFunitPrice(new BigDecimal("100"));
        value.setFtaxRate(new BigDecimal("0.13"));
        value.setFplannedDeliveryDate(LocalDate.of(2026, 9, 20));
        value.setForderedQuantity(BigDecimal.ZERO);
        value.setFversion(0);
        return value;
    }

    private PurchaseRequestEntryEntity requestEntry() {
        PurchaseRequestEntryEntity value = new PurchaseRequestEntryEntity();
        value.setFid(11L);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFpurchaseRequestId(1L);
        value.setFquantity(new BigDecimal("10"));
        value.setFsourcedQuantity(new BigDecimal("10"));
        value.setForderedQuantity(BigDecimal.ZERO);
        value.setFversion(0);
        return value;
    }

    private PurchaseRequestEntity request() {
        PurchaseRequestEntity value = new PurchaseRequestEntity();
        value.setFid(1L);
        value.setFtenantId("tenant-a");
        value.setForgId(1000L);
        value.setFexecutionStatus("CONTRACTING");
        value.setFversion(0);
        return value;
    }
}
