package single.cjj.erp.integration.botp;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.integration.botp.ProcurementBotpContracts.BotpDocumentResponse;
import single.cjj.erp.integration.botp.ProcurementBotpContracts.BotpTargetCreateRequest;
import single.cjj.erp.integration.botp.ProcurementBotpContracts.BotpTargetEntryResult;
import single.cjj.erp.integration.botp.ProcurementBotpContracts.BotpTargetResponse;
import single.cjj.erp.procurement.acceptance.dto.PurchaseAcceptanceContracts.PurchaseAcceptanceCreateRequest;
import single.cjj.erp.procurement.acceptance.dto.PurchaseAcceptanceContracts.PurchaseAcceptanceDetail;
import single.cjj.erp.procurement.acceptance.dto.PurchaseAcceptanceContracts.PurchaseAcceptanceEntryRequest;
import single.cjj.erp.procurement.acceptance.entity.PurchaseAcceptanceEntryEntity;
import single.cjj.erp.procurement.acceptance.service.PurchaseAcceptanceService;
import single.cjj.erp.procurement.inbound.dto.PurchaseInboundContracts.PurchaseInboundCreateRequest;
import single.cjj.erp.procurement.inbound.dto.PurchaseInboundContracts.PurchaseInboundDetail;
import single.cjj.erp.procurement.inbound.dto.PurchaseInboundContracts.PurchaseInboundEntryRequest;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntryEntity;
import single.cjj.erp.procurement.inbound.service.PurchaseInboundService;
import single.cjj.erp.procurement.request.dto.PurchaseRequestContracts;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntryEntity;
import single.cjj.erp.procurement.request.service.PurchaseRequestService;
import single.cjj.erp.procurement.sourcing.dto.SourcingContracts.AwardDetail;
import single.cjj.erp.procurement.sourcing.dto.SourcingContracts.RfqDetail;
import single.cjj.erp.procurement.sourcing.entity.ProcurementRfqEntryEntity;
import single.cjj.erp.procurement.sourcing.entity.SourcingAwardEntryEntity;
import single.cjj.erp.procurement.sourcing.service.ProcurementSourcingService;
import single.cjj.erp.procurement.contract.dto.PurchaseContractContracts;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntryEntity;
import single.cjj.erp.procurement.contract.service.PurchaseContractService;
import single.cjj.erp.procurement.delivery.dto.PurchaseDeliveryPlanContracts;
import single.cjj.erp.procurement.delivery.entity.PurchaseDeliveryPlanEntryEntity;
import single.cjj.erp.procurement.delivery.service.PurchaseDeliveryPlanService;
import single.cjj.erp.procurement.reverse.dto.PurchaseReverseContracts;
import single.cjj.erp.procurement.reverse.entity.PurchaseDeductionEntryEntity;
import single.cjj.erp.procurement.reverse.entity.PurchaseReturnEntryEntity;
import single.cjj.erp.procurement.reverse.entity.SupplierClaimEntryEntity;
import single.cjj.erp.procurement.reverse.service.PurchaseDeductionService;
import single.cjj.erp.procurement.reverse.service.PurchaseReturnService;
import single.cjj.erp.procurement.reverse.service.SupplierClaimService;
import single.cjj.erp.procurement.order.dto.PurchaseOrderContracts.PurchaseOrderDetail;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;
import single.cjj.erp.procurement.order.service.PurchaseOrderService;
import single.cjj.erp.procurement.receipt.dto.PurchaseReceiptContracts.PurchaseReceiptCreateRequest;
import single.cjj.erp.procurement.receipt.dto.PurchaseReceiptContracts.PurchaseReceiptDetail;
import single.cjj.erp.procurement.receipt.dto.PurchaseReceiptContracts.PurchaseReceiptEntryRequest;
import single.cjj.erp.procurement.receipt.entity.PurchaseReceiptEntryEntity;
import single.cjj.erp.procurement.receipt.service.PurchaseReceiptService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/procurement/internal/botp")
public class ProcurementBotpController {

    public static final String SYSTEM = "MATRIX";
    public static final String REQUEST = "ERP_PURCHASE_REQUEST";
    public static final String RFQ = "ERP_PROCUREMENT_RFQ";
    public static final String AWARD = "ERP_SOURCING_AWARD";
    public static final String CONTRACT = "ERP_PURCHASE_CONTRACT";
    public static final String ORDER = "ERP_PURCHASE_ORDER";
    public static final String DELIVERY_PLAN = "ERP_PURCHASE_DELIVERY_PLAN";
    public static final String RECEIPT = "ERP_PURCHASE_RECEIPT";
    public static final String ACCEPTANCE = "ERP_PURCHASE_ACCEPTANCE";
    public static final String INBOUND = "ERP_PURCHASE_INBOUND";
    public static final String PURCHASE_RETURN = "ERP_PURCHASE_RETURN";
    public static final String SUPPLIER_CLAIM = "ERP_SUPPLIER_CLAIM";
    public static final String PURCHASE_DEDUCTION = "ERP_PURCHASE_DEDUCTION";

    private final PurchaseRequestService requestService;
    private final ProcurementSourcingService sourcingService;
    private final PurchaseContractService contractService;
    private final PurchaseOrderService orderService;
    private final PurchaseDeliveryPlanService deliveryPlanService;
    private final PurchaseReceiptService receiptService;
    private final PurchaseAcceptanceService acceptanceService;
    private final PurchaseInboundService inboundService;
    private final PurchaseReturnService returnService;
    private final SupplierClaimService claimService;
    private final PurchaseDeductionService deductionService;

    public ProcurementBotpController(
            PurchaseRequestService requestService,
            ProcurementSourcingService sourcingService,
            PurchaseContractService contractService,
            PurchaseOrderService orderService,
            PurchaseDeliveryPlanService deliveryPlanService,
            PurchaseReceiptService receiptService,
            PurchaseAcceptanceService acceptanceService,
            PurchaseInboundService inboundService,
            PurchaseReturnService returnService,
            SupplierClaimService claimService,
            PurchaseDeductionService deductionService
    ) {
        this.requestService = requestService;
        this.sourcingService = sourcingService;
        this.contractService = contractService;
        this.orderService = orderService;
        this.deliveryPlanService = deliveryPlanService;
        this.receiptService = receiptService;
        this.acceptanceService = acceptanceService;
        this.inboundService = inboundService;
        this.returnService = returnService;
        this.claimService = claimService;
        this.deductionService = deductionService;
    }

    @GetMapping("/documents/{documentType}/{fid}")
    public ApiResponse<BotpDocumentResponse> document(
            @PathVariable("documentType") String documentType,
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId
    ) {
        return ApiResponse.success(switch (documentType) {
            case REQUEST -> requestDocument(requestService.detail(fid, tenantId));
            case RFQ -> rfqDocument(sourcingService.rfqDetail(fid, tenantId));
            case AWARD -> awardDocument(sourcingService.awardDetail(fid, tenantId));
            case CONTRACT -> contractDocument(contractService.detail(fid, tenantId));
            case ORDER -> orderDocument(orderService.detail(fid, tenantId));
            case DELIVERY_PLAN -> deliveryPlanDocument(deliveryPlanService.detail(fid, tenantId));
            case RECEIPT -> receiptDocument(receiptService.detail(fid, tenantId));
            case ACCEPTANCE -> acceptanceDocument(acceptanceService.detail(fid, tenantId));
            case INBOUND -> inboundDocument(inboundService.detail(fid, tenantId));
            case PURCHASE_RETURN -> returnDocument(returnService.detail(fid, tenantId));
            case SUPPLIER_CLAIM -> claimDocument(claimService.detail(fid, tenantId));
            case PURCHASE_DEDUCTION -> deductionDocument(deductionService.detail(fid, tenantId));
            default -> throw new BizException("不支持的采购 BOTP 单据类型: " + documentType);
        });
    }

    @GetMapping("/targets/{documentType}/by-idempotency")
    public ApiResponse<BotpTargetResponse> findByIdempotency(
            @PathVariable("documentType") String documentType,
            @RequestParam("key") String key
    ) {
        return ApiResponse.success(switch (documentType) {
            case RECEIPT -> toRecoveredTarget(receiptService.findByIdempotencyKey(key), RECEIPT);
            case ACCEPTANCE -> toRecoveredTarget(acceptanceService.findByIdempotencyKey(key), ACCEPTANCE);
            case INBOUND -> toRecoveredTarget(inboundService.findByIdempotencyKey(key), INBOUND);
            default -> throw new BizException("该采购单据类型不支持作为 BOTP 目标: " + documentType);
        });
    }

    @PostMapping("/targets/{documentType}")
    public ApiResponse<BotpTargetResponse> createTarget(
            @PathVariable("documentType") String documentType,
            @Valid @RequestBody BotpTargetCreateRequest request
    ) {
        Map<String, Object> header = request.header() == null ? Map.of() : request.header();
        List<Map<String, Object>> entries = request.entries() == null ? List.of() : request.entries();
        Long operatorId = optionalLong(header.get("operatorId"));
        return ApiResponse.success(switch (documentType) {
            case RECEIPT -> {
                PurchaseReceiptDetail detail = receiptService.create(toReceiptCreate(request, header, entries), operatorId);
                yield toTarget(detail, correlationKeys(entries), RECEIPT);
            }
            case ACCEPTANCE -> {
                PurchaseAcceptanceDetail detail = acceptanceService.create(toAcceptanceCreate(request, header, entries), operatorId);
                yield toTarget(detail, correlationKeys(entries), ACCEPTANCE);
            }
            case INBOUND -> {
                PurchaseInboundDetail detail = inboundService.create(toInboundCreate(request, header, entries), operatorId);
                yield toTarget(detail, correlationKeys(entries), INBOUND);
            }
            default -> throw new BizException("该采购单据类型不支持作为 BOTP 目标: " + documentType);
        });
    }

    private BotpDocumentResponse requestDocument(PurchaseRequestContracts.Detail detail) {
        var request = detail.request();
        Map<String, Object> header = commonHeader(
                request.getFtenantId(), request.getForgId(), request.getFnumber(), request.getFdate(),
                null, null, null, request.getFcurrencyCode(), request.getFstatus(), request.getFapprovalStatus());
        header.put("executionStatus", request.getFexecutionStatus());
        header.put("requesterId", request.getFrequesterId());
        header.put("requestDepartmentId", request.getFrequestDepartmentId());
        header.put("requiredDate", request.getFrequiredDate());
        header.put("projectId", request.getFprojectId());
        header.put("costCenterId", request.getFcostCenterId());
        List<Map<String, Object>> entries = detail.entries().stream().map(this::requestEntry).toList();
        return new BotpDocumentResponse(SYSTEM, REQUEST, String.valueOf(request.getFid()), request.getFnumber(), header, entries);
    }

    private Map<String, Object> requestEntry(PurchaseRequestEntryEntity entry) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("entryId", String.valueOf(entry.getFid()));
        line.put("purchaseRequestId", entry.getFpurchaseRequestId());
        line.put("purchaseRequestEntryId", entry.getFid());
        line.put("materialId", entry.getFmaterialId());
        line.put("materialCode", entry.getFmaterialCode());
        line.put("materialName", entry.getFmaterialName());
        line.put("specification", entry.getFspecification());
        line.put("unitId", entry.getFunitId());
        line.put("quantity", entry.getFquantity());
        line.put("availableQuantity", nz(entry.getFquantity()).subtract(nz(entry.getFsourcedQuantity())));
        line.put("estimatedUnitPrice", entry.getFestimatedUnitPrice());
        line.put("estimatedAmount", entry.getFestimatedAmount());
        line.put("requiredDate", entry.getFrequiredDate());
        line.put("projectId", entry.getFprojectId());
        line.put("costCenterId", entry.getFcostCenterId());
        return line;
    }

    private BotpDocumentResponse rfqDocument(RfqDetail detail) {
        var rfq = detail.rfq();
        Map<String, Object> header = commonHeader(
                rfq.getFtenantId(), rfq.getForgId(), rfq.getFnumber(), rfq.getFdate(),
                null, null, null, rfq.getFcurrencyCode(), rfq.getFstatus(), null);
        header.put("title", rfq.getFtitle());
        header.put("quotationDeadline", rfq.getFquotationDeadline());
        header.put("supplierCount", detail.suppliers().size());
        List<Map<String, Object>> entries = detail.entries().stream().map(this::rfqEntry).toList();
        return new BotpDocumentResponse(SYSTEM, RFQ, String.valueOf(rfq.getFid()), rfq.getFnumber(), header, entries);
    }

    private Map<String, Object> rfqEntry(ProcurementRfqEntryEntity entry) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("entryId", String.valueOf(entry.getFid()));
        line.put("rfqId", entry.getFrfqId());
        line.put("rfqEntryId", entry.getFid());
        line.put("purchaseRequestId", entry.getFpurchaseRequestId());
        line.put("purchaseRequestEntryId", entry.getFpurchaseRequestEntryId());
        line.put("materialId", entry.getFmaterialId());
        line.put("materialCode", entry.getFmaterialCode());
        line.put("materialName", entry.getFmaterialName());
        line.put("specification", entry.getFspecification());
        line.put("unitId", entry.getFunitId());
        line.put("quantity", entry.getFquantity());
        line.put("availableQuantity", nz(entry.getFquantity()).subtract(nz(entry.getFawardedQuantity())));
        line.put("requiredDate", entry.getFrequiredDate());
        line.put("projectId", entry.getFprojectId());
        line.put("costCenterId", entry.getFcostCenterId());
        return line;
    }

    private BotpDocumentResponse awardDocument(AwardDetail detail) {
        var award = detail.award();
        Map<String, Object> header = commonHeader(
                award.getFtenantId(), award.getForgId(), award.getFnumber(), award.getFdate(),
                null, null, null, null, award.getFstatus(), null);
        header.put("rfqId", award.getFrfqId());
        header.put("grossAmount", award.getFgrossAmount());
        List<Map<String, Object>> entries = detail.entries().stream().map(this::awardEntry).toList();
        return new BotpDocumentResponse(SYSTEM, AWARD, String.valueOf(award.getFid()), award.getFnumber(), header, entries);
    }

    private Map<String, Object> awardEntry(SourcingAwardEntryEntity entry) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("entryId", String.valueOf(entry.getFid()));
        line.put("sourcingAwardId", entry.getFawardId());
        line.put("sourcingAwardEntryId", entry.getFid());
        line.put("rfqEntryId", entry.getFrfqEntryId());
        line.put("quoteId", entry.getFquoteId());
        line.put("quoteEntryId", entry.getFquoteEntryId());
        line.put("businessPartnerId", entry.getFbusinessPartnerId());
        line.put("businessPartnerCode", entry.getFbusinessPartnerCode());
        line.put("businessPartnerName", entry.getFbusinessPartnerName());
        line.put("quantity", entry.getFawardedQuantity());
        line.put("availableQuantity", entry.getFawardedQuantity());
        line.put("unitPrice", entry.getFunitPrice());
        line.put("taxRate", entry.getFtaxRate());
        line.put("netAmount", entry.getFnetAmount());
        line.put("taxAmount", entry.getFtaxAmount());
        line.put("amount", entry.getFgrossAmount());
        return line;
    }

    private BotpDocumentResponse contractDocument(PurchaseContractContracts.Detail detail) {
        var contract = detail.contract();
        Map<String, Object> header = commonHeader(
                contract.getFtenantId(), contract.getForgId(), contract.getFnumber(), contract.getFdate(),
                contract.getFbusinessPartnerId(), contract.getFbusinessPartnerCode(), contract.getFbusinessPartnerName(),
                contract.getFcurrencyCode(), contract.getFstatus(), contract.getFapprovalStatus());
        header.put("sourcingAwardId", contract.getFsourcingAwardId());
        header.put("executionStatus", contract.getFexecutionStatus());
        header.put("startDate", contract.getFstartDate());
        header.put("endDate", contract.getFendDate());
        header.put("paymentTermCode", contract.getFpaymentTermCode());
        header.put("deliveryTermCode", contract.getFdeliveryTermCode());
        List<Map<String, Object>> entries = detail.entries().stream().map(this::contractEntry).toList();
        return new BotpDocumentResponse(SYSTEM, CONTRACT, String.valueOf(contract.getFid()), contract.getFnumber(), header, entries);
    }

    private Map<String, Object> contractEntry(PurchaseContractEntryEntity entry) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("entryId", String.valueOf(entry.getFid()));
        line.put("purchaseContractId", entry.getFpurchaseContractId());
        line.put("contractEntryId", entry.getFid());
        line.put("sourcingAwardEntryId", entry.getFsourcingAwardEntryId());
        line.put("rfqEntryId", entry.getFrfqEntryId());
        line.put("purchaseRequestId", entry.getFpurchaseRequestId());
        line.put("purchaseRequestEntryId", entry.getFpurchaseRequestEntryId());
        line.put("materialId", entry.getFmaterialId());
        line.put("materialCode", entry.getFmaterialCode());
        line.put("materialName", entry.getFmaterialName());
        line.put("specification", entry.getFspecification());
        line.put("unitId", entry.getFunitId());
        line.put("quantity", entry.getFquantity());
        line.put("availableQuantity", nz(entry.getFquantity()).subtract(nz(entry.getForderedQuantity())));
        line.put("unitPrice", entry.getFunitPrice());
        line.put("taxRate", entry.getFtaxRate());
        line.put("amount", entry.getFgrossAmount());
        line.put("plannedDeliveryDate", entry.getFplannedDeliveryDate());
        line.put("projectId", entry.getFprojectId());
        line.put("costCenterId", entry.getFcostCenterId());
        return line;
    }

    private BotpDocumentResponse deliveryPlanDocument(PurchaseDeliveryPlanContracts.Detail detail) {
        var plan = detail.plan();
        Map<String, Object> header = commonHeader(
                plan.getFtenantId(), plan.getForgId(), plan.getFnumber(), plan.getFdate(),
                plan.getFbusinessPartnerId(), plan.getFbusinessPartnerCode(), plan.getFbusinessPartnerName(),
                plan.getFcurrencyCode(), plan.getFstatus(), null);
        header.put("purchaseOrderId", plan.getFpurchaseOrderId());
        header.put("purchaseOrderNo", plan.getFpurchaseOrderNo());
        List<Map<String, Object>> entries = detail.entries().stream().map(this::deliveryPlanEntry).toList();
        return new BotpDocumentResponse(SYSTEM, DELIVERY_PLAN, String.valueOf(plan.getFid()), plan.getFnumber(), header, entries);
    }

    private Map<String, Object> deliveryPlanEntry(PurchaseDeliveryPlanEntryEntity entry) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("entryId", String.valueOf(entry.getFid()));
        line.put("deliveryPlanId", entry.getFdeliveryPlanId());
        line.put("deliveryPlanEntryId", entry.getFid());
        line.put("purchaseOrderId", entry.getFpurchaseOrderId());
        line.put("purchaseOrderEntryId", entry.getFpurchaseOrderEntryId());
        line.put("materialId", entry.getFmaterialId());
        line.put("materialCode", entry.getFmaterialCode());
        line.put("materialName", entry.getFmaterialName());
        line.put("quantity", entry.getFplannedQuantity());
        line.put("plannedDeliveryDate", entry.getFplannedDeliveryDate());
        line.put("committedQuantity", entry.getFcommittedQuantity());
        line.put("committedDeliveryDate", entry.getFcommittedDeliveryDate());
        line.put("receivedQuantity", entry.getFreceivedQuantity());
        return line;
    }

    private BotpDocumentResponse returnDocument(PurchaseReverseContracts.ReturnDetail detail) {
        var headerEntity = detail.header();
        Map<String, Object> header = commonHeader(
                headerEntity.getFtenantId(), headerEntity.getForgId(), headerEntity.getFnumber(), headerEntity.getFdate(),
                headerEntity.getFbusinessPartnerId(), headerEntity.getFbusinessPartnerCode(), headerEntity.getFbusinessPartnerName(),
                headerEntity.getFcurrencyCode(), headerEntity.getFstatus(), headerEntity.getFapprovalStatus());
        header.put("purchaseInboundId", headerEntity.getFpurchaseInboundId());
        header.put("purchaseOrderId", headerEntity.getFpurchaseOrderId());
        header.put("reasonType", headerEntity.getFreasonType());
        List<Map<String, Object>> entries = detail.entries().stream().map(this::returnEntry).toList();
        return new BotpDocumentResponse(SYSTEM, PURCHASE_RETURN, String.valueOf(headerEntity.getFid()), headerEntity.getFnumber(), header, entries);
    }

    private Map<String, Object> returnEntry(PurchaseReturnEntryEntity entry) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("entryId", String.valueOf(entry.getFid()));
        line.put("purchaseReturnId", entry.getFpurchaseReturnId());
        line.put("purchaseInboundId", entry.getFpurchaseInboundId());
        line.put("purchaseInboundEntryId", entry.getFpurchaseInboundEntryId());
        line.put("purchaseOrderId", entry.getFpurchaseOrderId());
        line.put("purchaseOrderEntryId", entry.getFpurchaseOrderEntryId());
        line.put("materialId", entry.getFmaterialId());
        line.put("materialCode", entry.getFmaterialCode());
        line.put("materialName", entry.getFmaterialName());
        line.put("quantity", entry.getFquantity());
        line.put("unitPrice", entry.getFunitPrice());
        line.put("amount", entry.getFamount());
        return line;
    }

    private BotpDocumentResponse claimDocument(PurchaseReverseContracts.ClaimDetail detail) {
        var headerEntity = detail.header();
        Map<String, Object> header = commonHeader(
                headerEntity.getFtenantId(), headerEntity.getForgId(), headerEntity.getFnumber(), headerEntity.getFdate(),
                headerEntity.getFbusinessPartnerId(), headerEntity.getFbusinessPartnerCode(), headerEntity.getFbusinessPartnerName(),
                headerEntity.getFcurrencyCode(), headerEntity.getFstatus(), headerEntity.getFapprovalStatus());
        header.put("purchaseOrderId", headerEntity.getFpurchaseOrderId());
        header.put("purchaseReturnId", headerEntity.getFpurchaseReturnId());
        header.put("claimType", headerEntity.getFclaimType());
        header.put("requestedAmount", headerEntity.getFrequestedAmount());
        header.put("agreedAmount", headerEntity.getFagreedAmount());
        List<Map<String, Object>> entries = detail.entries().stream().map(this::claimEntry).toList();
        return new BotpDocumentResponse(SYSTEM, SUPPLIER_CLAIM, String.valueOf(headerEntity.getFid()), headerEntity.getFnumber(), header, entries);
    }

    private Map<String, Object> claimEntry(SupplierClaimEntryEntity entry) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("entryId", String.valueOf(entry.getFid()));
        line.put("supplierClaimId", entry.getFsupplierClaimId());
        line.put("purchaseOrderId", entry.getFpurchaseOrderId());
        line.put("purchaseOrderEntryId", entry.getFpurchaseOrderEntryId());
        line.put("purchaseReturnEntryId", entry.getFpurchaseReturnEntryId());
        line.put("materialId", entry.getFmaterialId());
        line.put("materialCode", entry.getFmaterialCode());
        line.put("materialName", entry.getFmaterialName());
        line.put("requestedAmount", entry.getFrequestedAmount());
        line.put("agreedAmount", entry.getFagreedAmount());
        line.put("amount", entry.getFagreedAmount());
        return line;
    }

    private BotpDocumentResponse deductionDocument(PurchaseReverseContracts.DeductionDetail detail) {
        var headerEntity = detail.header();
        Map<String, Object> header = commonHeader(
                headerEntity.getFtenantId(), headerEntity.getForgId(), headerEntity.getFnumber(), headerEntity.getFdate(),
                headerEntity.getFbusinessPartnerId(), headerEntity.getFbusinessPartnerCode(), headerEntity.getFbusinessPartnerName(),
                headerEntity.getFcurrencyCode(), headerEntity.getFstatus(), headerEntity.getFapprovalStatus());
        header.put("supplierClaimId", headerEntity.getFsupplierClaimId());
        header.put("purchaseOrderId", headerEntity.getFpurchaseOrderId());
        header.put("amount", headerEntity.getFamount());
        List<Map<String, Object>> entries = detail.entries().stream().map(this::deductionEntry).toList();
        return new BotpDocumentResponse(SYSTEM, PURCHASE_DEDUCTION, String.valueOf(headerEntity.getFid()), headerEntity.getFnumber(), header, entries);
    }

    private Map<String, Object> deductionEntry(PurchaseDeductionEntryEntity entry) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("entryId", String.valueOf(entry.getFid()));
        line.put("purchaseDeductionId", entry.getFpurchaseDeductionId());
        line.put("supplierClaimEntryId", entry.getFsupplierClaimEntryId());
        line.put("purchaseOrderId", entry.getFpurchaseOrderId());
        line.put("purchaseOrderEntryId", entry.getFpurchaseOrderEntryId());
        line.put("materialId", entry.getFmaterialId());
        line.put("materialCode", entry.getFmaterialCode());
        line.put("materialName", entry.getFmaterialName());
        line.put("amount", entry.getFamount());
        return line;
    }

    private BotpDocumentResponse orderDocument(PurchaseOrderDetail detail) {
        var order = detail.order();
        Map<String, Object> header = commonHeader(
                order.getFtenantId(), order.getForgId(), order.getFnumber(), order.getFdate(),
                order.getFbusinessPartnerId(), order.getFbusinessPartnerCode(), order.getFbusinessPartnerName(),
                order.getFcurrencyCode(), order.getFstatus(), order.getFapprovalStatus());
        header.put("contractId", order.getFcontractId());
        header.put("paymentTermCode", order.getFpaymentTermCode());
        header.put("plannedDeliveryDate", order.getFplannedDeliveryDate());
        List<Map<String, Object>> entries = detail.entries().stream().map(this::orderEntry).toList();
        return new BotpDocumentResponse(SYSTEM, ORDER, String.valueOf(order.getFid()), order.getFnumber(), header, entries);
    }

    private Map<String, Object> orderEntry(PurchaseOrderEntryEntity entry) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("entryId", String.valueOf(entry.getFid()));
        line.put("purchaseOrderId", entry.getFpurchaseOrderId());
        line.put("purchaseOrderEntryId", entry.getFid());
        line.put("contractEntryId", entry.getFcontractEntryId());
        line.put("sourcingAwardEntryId", entry.getFsourcingAwardEntryId());
        line.put("rfqEntryId", entry.getFrfqEntryId());
        line.put("purchaseRequestId", entry.getFpurchaseRequestId());
        line.put("purchaseRequestEntryId", entry.getFpurchaseRequestEntryId());
        line.put("materialId", entry.getFmaterialId());
        line.put("materialCode", entry.getFmaterialCode());
        line.put("materialName", entry.getFmaterialName());
        line.put("specification", entry.getFspecification());
        line.put("unitId", entry.getFunitId());
        line.put("quantity", entry.getFquantity());
        line.put("availableQuantity", nz(entry.getFquantity())
                .subtract(nz(entry.getFreceivedQuantity()))
                .subtract(nz(entry.getFreceiptReservedQuantity())));
        line.put("unitPrice", entry.getFunitPrice());
        line.put("taxRate", entry.getFtaxRate());
        line.put("plannedDeliveryDate", entry.getFplannedDeliveryDate());
        line.put("projectId", entry.getFprojectId());
        line.put("costCenterId", entry.getFcostCenterId());
        return line;
    }

    private BotpDocumentResponse receiptDocument(PurchaseReceiptDetail detail) {
        var receipt = detail.receipt();
        Map<String, Object> header = commonHeader(
                receipt.getFtenantId(), receipt.getForgId(), receipt.getFnumber(), receipt.getFdate(),
                receipt.getFbusinessPartnerId(), receipt.getFbusinessPartnerCode(), receipt.getFbusinessPartnerName(),
                receipt.getFcurrencyCode(), receipt.getFstatus(), receipt.getFapprovalStatus());
        header.put("purchaseReceiptId", receipt.getFid());
        header.put("warehouseId", receipt.getFwarehouseId());
        List<Map<String, Object>> entries = detail.entries().stream().map(this::receiptEntry).toList();
        return new BotpDocumentResponse(SYSTEM, RECEIPT, String.valueOf(receipt.getFid()), receipt.getFnumber(), header, entries);
    }

    private Map<String, Object> receiptEntry(PurchaseReceiptEntryEntity entry) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("entryId", String.valueOf(entry.getFid()));
        line.put("purchaseReceiptId", entry.getFpurchaseReceiptId());
        line.put("purchaseReceiptEntryId", entry.getFid());
        line.put("purchaseOrderId", entry.getFpurchaseOrderId());
        line.put("purchaseOrderEntryId", entry.getFpurchaseOrderEntryId());
        line.put("materialId", entry.getFmaterialId());
        line.put("materialCode", entry.getFmaterialCode());
        line.put("materialName", entry.getFmaterialName());
        line.put("specification", entry.getFspecification());
        line.put("unitId", entry.getFunitId());
        line.put("quantity", entry.getFquantity());
        line.put("availableQuantity", nz(entry.getFquantity())
                .subtract(nz(entry.getFinspectedQuantity()))
                .subtract(nz(entry.getFinspectionReservedQuantity())));
        line.put("batchNo", entry.getFbatchNo());
        line.put("warehouseId", entry.getFwarehouseId());
        line.put("projectId", entry.getFprojectId());
        line.put("costCenterId", entry.getFcostCenterId());
        return line;
    }

    private BotpDocumentResponse acceptanceDocument(PurchaseAcceptanceDetail detail) {
        var acceptance = detail.acceptance();
        Map<String, Object> header = commonHeader(
                acceptance.getFtenantId(), acceptance.getForgId(), acceptance.getFnumber(), acceptance.getFdate(),
                acceptance.getFbusinessPartnerId(), acceptance.getFbusinessPartnerCode(), acceptance.getFbusinessPartnerName(),
                acceptance.getFcurrencyCode(), acceptance.getFstatus(), acceptance.getFapprovalStatus());
        header.put("purchaseAcceptanceId", acceptance.getFid());
        header.put("purchaseReceiptId", acceptance.getFpurchaseReceiptId());
        header.put("result", acceptance.getFresult());
        List<Map<String, Object>> entries = detail.entries().stream().map(this::acceptanceEntry).toList();
        return new BotpDocumentResponse(SYSTEM, ACCEPTANCE, String.valueOf(acceptance.getFid()), acceptance.getFnumber(), header, entries);
    }

    private Map<String, Object> acceptanceEntry(PurchaseAcceptanceEntryEntity entry) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("entryId", String.valueOf(entry.getFid()));
        line.put("purchaseAcceptanceId", entry.getFpurchaseAcceptanceId());
        line.put("purchaseAcceptanceEntryId", entry.getFid());
        line.put("purchaseReceiptEntryId", entry.getFpurchaseReceiptEntryId());
        line.put("purchaseOrderId", entry.getFpurchaseOrderId());
        line.put("purchaseOrderEntryId", entry.getFpurchaseOrderEntryId());
        line.put("materialId", entry.getFmaterialId());
        line.put("materialCode", entry.getFmaterialCode());
        line.put("materialName", entry.getFmaterialName());
        line.put("specification", entry.getFspecification());
        line.put("unitId", entry.getFunitId());
        line.put("inspectionQuantity", entry.getFinspectionQuantity());
        line.put("qualifiedQuantity", entry.getFqualifiedQuantity());
        line.put("concessionQuantity", entry.getFconcessionQuantity());
        line.put("rejectedQuantity", entry.getFrejectedQuantity());
        BigDecimal accepted = nz(entry.getFqualifiedQuantity()).add(nz(entry.getFconcessionQuantity()));
        line.put("availableQuantity", accepted
                .subtract(nz(entry.getFinboundQuantity()))
                .subtract(nz(entry.getFinboundReservedQuantity())));
        line.put("quantity", accepted);
        line.put("unitPrice", entry.getFunitPrice());
        line.put("batchNo", entry.getFbatchNo());
        line.put("projectId", entry.getFprojectId());
        line.put("costCenterId", entry.getFcostCenterId());
        return line;
    }

    private BotpDocumentResponse inboundDocument(PurchaseInboundDetail detail) {
        var inbound = detail.inbound();
        Map<String, Object> header = commonHeader(
                inbound.getFtenantId(), inbound.getForgId(), inbound.getFnumber(), inbound.getFdate(),
                inbound.getFbusinessPartnerId(), inbound.getFbusinessPartnerCode(), inbound.getFbusinessPartnerName(),
                inbound.getFcurrencyCode(), inbound.getFstatus(), inbound.getFapprovalStatus());
        header.put("purchaseInboundId", inbound.getFid());
        header.put("purchaseAcceptanceId", inbound.getFpurchaseAcceptanceId());
        header.put("warehouseId", inbound.getFwarehouseId());
        header.put("totalQuantity", inbound.getFtotalQuantity());
        header.put("totalAmount", inbound.getFtotalAmount());
        List<Map<String, Object>> entries = detail.entries().stream().map(this::inboundEntry).toList();
        return new BotpDocumentResponse(SYSTEM, INBOUND, String.valueOf(inbound.getFid()), inbound.getFnumber(), header, entries);
    }

    private Map<String, Object> inboundEntry(PurchaseInboundEntryEntity entry) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("entryId", String.valueOf(entry.getFid()));
        line.put("purchaseInboundId", entry.getFpurchaseInboundId());
        line.put("purchaseInboundEntryId", entry.getFid());
        line.put("purchaseAcceptanceEntryId", entry.getFpurchaseAcceptanceEntryId());
        line.put("purchaseReceiptEntryId", entry.getFpurchaseReceiptEntryId());
        line.put("purchaseOrderId", entry.getFpurchaseOrderId());
        line.put("purchaseOrderEntryId", entry.getFpurchaseOrderEntryId());
        line.put("materialId", entry.getFmaterialId());
        line.put("materialCode", entry.getFmaterialCode());
        line.put("materialName", entry.getFmaterialName());
        line.put("quantity", entry.getFquantity());
        line.put("unitPrice", entry.getFunitPrice());
        line.put("amount", entry.getFamount());
        line.put("batchNo", entry.getFbatchNo());
        line.put("warehouseId", entry.getFwarehouseId());
        line.put("projectId", entry.getFprojectId());
        line.put("costCenterId", entry.getFcostCenterId());
        return line;
    }

    private PurchaseReceiptCreateRequest toReceiptCreate(
            BotpTargetCreateRequest request,
            Map<String, Object> header,
            List<Map<String, Object>> entries
    ) {
        return new PurchaseReceiptCreateRequest(
                text(header, "tenantId"),
                requiredLong(header, "orgId"),
                optionalText(header.get("number")),
                localDate(header.get("date")),
                requiredLong(header, "businessPartnerId"),
                text(header, "businessPartnerCode"),
                text(header, "businessPartnerName"),
                text(header, "currencyCode"),
                optionalText(header.get("supplierDeliveryNo")),
                optionalLong(header.get("warehouseId")),
                request.idempotencyKey(),
                optionalText(header.get("sourceExecutionId")),
                entries.stream().map(item -> new PurchaseReceiptEntryRequest(
                        requiredLong(item, "purchaseOrderEntryId"),
                        decimal(item.get("quantity"), "quantity"),
                        optionalText(item.get("batchNo")),
                        optionalLong(item.get("warehouseId"))
                )).toList()
        );
    }

    private PurchaseAcceptanceCreateRequest toAcceptanceCreate(
            BotpTargetCreateRequest request,
            Map<String, Object> header,
            List<Map<String, Object>> entries
    ) {
        return new PurchaseAcceptanceCreateRequest(
                text(header, "tenantId"),
                requiredLong(header, "orgId"),
                optionalText(header.get("number")),
                localDate(header.get("date")),
                requiredLong(header, "purchaseReceiptId"),
                requiredLong(header, "businessPartnerId"),
                text(header, "businessPartnerCode"),
                text(header, "businessPartnerName"),
                text(header, "currencyCode"),
                request.idempotencyKey(),
                optionalText(header.get("sourceExecutionId")),
                entries.stream().map(item -> new PurchaseAcceptanceEntryRequest(
                        requiredLong(item, "purchaseReceiptEntryId"),
                        decimal(item.get("inspectionQuantity"), "inspectionQuantity"),
                        optionalDecimal(item.get("qualifiedQuantity")),
                        optionalDecimal(item.get("concessionQuantity")),
                        optionalDecimal(item.get("rejectedQuantity")),
                        optionalText(item.get("inspectionMethod")),
                        null
                )).toList()
        );
    }

    private PurchaseInboundCreateRequest toInboundCreate(
            BotpTargetCreateRequest request,
            Map<String, Object> header,
            List<Map<String, Object>> entries
    ) {
        return new PurchaseInboundCreateRequest(
                text(header, "tenantId"),
                requiredLong(header, "orgId"),
                optionalText(header.get("number")),
                localDate(header.get("date")),
                requiredLong(header, "purchaseAcceptanceId"),
                requiredLong(header, "businessPartnerId"),
                text(header, "businessPartnerCode"),
                text(header, "businessPartnerName"),
                text(header, "currencyCode"),
                optionalLong(header.get("warehouseId")),
                request.idempotencyKey(),
                optionalText(header.get("sourceExecutionId")),
                entries.stream().map(item -> new PurchaseInboundEntryRequest(
                        requiredLong(item, "purchaseAcceptanceEntryId"),
                        decimal(item.get("quantity"), "quantity"),
                        optionalText(item.get("batchNo")),
                        optionalLong(item.get("warehouseId"))
                )).toList()
        );
    }

    private BotpTargetResponse toRecoveredTarget(PurchaseReceiptDetail detail, String type) {
        if (detail == null) {
            return null;
        }
        List<String> keys = detail.entries().stream()
                .map(PurchaseReceiptEntryEntity::getFpurchaseOrderEntryId)
                .map(String::valueOf)
                .toList();
        return toTarget(detail, keys, type);
    }

    private BotpTargetResponse toRecoveredTarget(PurchaseAcceptanceDetail detail, String type) {
        if (detail == null) {
            return null;
        }
        List<String> keys = detail.entries().stream()
                .map(PurchaseAcceptanceEntryEntity::getFpurchaseReceiptEntryId)
                .map(String::valueOf)
                .toList();
        return toTarget(detail, keys, type);
    }

    private BotpTargetResponse toRecoveredTarget(PurchaseInboundDetail detail, String type) {
        if (detail == null) {
            return null;
        }
        List<String> keys = detail.entries().stream()
                .map(PurchaseInboundEntryEntity::getFpurchaseAcceptanceEntryId)
                .map(String::valueOf)
                .toList();
        return toTarget(detail, keys, type);
    }

    private BotpTargetResponse toTarget(PurchaseReceiptDetail detail, List<String> correlationKeys, String type) {
        if (detail == null) {
            return null;
        }
        return new BotpTargetResponse(
                SYSTEM, type, String.valueOf(detail.receipt().getFid()), detail.receipt().getFnumber(),
                targetEntries(correlationKeys, detail.entries().stream().map(PurchaseReceiptEntryEntity::getFid).toList())
        );
    }

    private BotpTargetResponse toTarget(PurchaseAcceptanceDetail detail, List<String> correlationKeys, String type) {
        if (detail == null) {
            return null;
        }
        return new BotpTargetResponse(
                SYSTEM, type, String.valueOf(detail.acceptance().getFid()), detail.acceptance().getFnumber(),
                targetEntries(correlationKeys, detail.entries().stream().map(PurchaseAcceptanceEntryEntity::getFid).toList())
        );
    }

    private BotpTargetResponse toTarget(PurchaseInboundDetail detail, List<String> correlationKeys, String type) {
        if (detail == null) {
            return null;
        }
        return new BotpTargetResponse(
                SYSTEM, type, String.valueOf(detail.inbound().getFid()), detail.inbound().getFnumber(),
                targetEntries(correlationKeys, detail.entries().stream().map(PurchaseInboundEntryEntity::getFid).toList())
        );
    }

    private List<BotpTargetEntryResult> targetEntries(List<String> correlationKeys, List<Long> ids) {
        List<BotpTargetEntryResult> results = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            String correlation = i < correlationKeys.size() ? correlationKeys.get(i) : String.valueOf(i + 1);
            results.add(new BotpTargetEntryResult(correlation, String.valueOf(ids.get(i))));
        }
        return results;
    }

    private List<String> correlationKeys(List<Map<String, Object>> entries) {
        List<String> keys = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            Object value = entries.get(i).get("_botpCorrelationKey");
            keys.add(value == null ? String.valueOf(i + 1) : String.valueOf(value));
        }
        return keys;
    }

    private Map<String, Object> commonHeader(
            String tenantId,
            Long orgId,
            String number,
            LocalDate date,
            Long partnerId,
            String partnerCode,
            String partnerName,
            String currencyCode,
            String status,
            String approvalStatus
    ) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("tenantId", tenantId);
        header.put("orgId", orgId);
        header.put("number", number);
        header.put("date", date);
        header.put("businessPartnerId", partnerId);
        header.put("businessPartnerCode", partnerCode);
        header.put("businessPartnerName", partnerName);
        header.put("currencyCode", currencyCode);
        header.put("status", status);
        header.put("approvalStatus", approvalStatus);
        return header;
    }

    private String text(Map<String, Object> source, String field) {
        String value = optionalText(source.get(field));
        if (value == null || value.isBlank()) {
            throw new BizException(field + " 不能为空");
        }
        return value;
    }

    private Long requiredLong(Map<String, Object> source, String field) {
        Long value = optionalLong(source.get(field));
        if (value == null) {
            throw new BizException(field + " 不能为空");
        }
        return value;
    }

    private Long optionalLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new BizException("无效整数: " + value);
        }
    }

    private BigDecimal decimal(Object value, String field) {
        BigDecimal result = optionalDecimal(value);
        if (result == null) {
            throw new BizException(field + " 不能为空");
        }
        return result;
    }

    private BigDecimal optionalDecimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new BizException("无效数值: " + value);
        }
    }

    private LocalDate localDate(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return LocalDate.now();
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        try {
            return LocalDate.parse(String.valueOf(value));
        } catch (RuntimeException exception) {
            throw new BizException("date 必须是 yyyy-MM-dd");
        }
    }

    private String optionalText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}