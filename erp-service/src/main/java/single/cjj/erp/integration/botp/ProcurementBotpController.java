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
    public static final String ORDER = "ERP_PURCHASE_ORDER";
    public static final String RECEIPT = "ERP_PURCHASE_RECEIPT";
    public static final String ACCEPTANCE = "ERP_PURCHASE_ACCEPTANCE";
    public static final String INBOUND = "ERP_PURCHASE_INBOUND";

    private final PurchaseOrderService orderService;
    private final PurchaseReceiptService receiptService;
    private final PurchaseAcceptanceService acceptanceService;
    private final PurchaseInboundService inboundService;

    public ProcurementBotpController(
            PurchaseOrderService orderService,
            PurchaseReceiptService receiptService,
            PurchaseAcceptanceService acceptanceService,
            PurchaseInboundService inboundService
    ) {
        this.orderService = orderService;
        this.receiptService = receiptService;
        this.acceptanceService = acceptanceService;
        this.inboundService = inboundService;
    }

    @GetMapping("/documents/{documentType}/{fid}")
    public ApiResponse<BotpDocumentResponse> document(
            @PathVariable("documentType") String documentType,
            @PathVariable("fid") Long fid,
            @RequestParam("tenantId") String tenantId
    ) {
        return ApiResponse.success(switch (documentType) {
            case ORDER -> orderDocument(orderService.detail(fid, tenantId));
            case RECEIPT -> receiptDocument(receiptService.detail(fid, tenantId));
            case ACCEPTANCE -> acceptanceDocument(acceptanceService.detail(fid, tenantId));
            case INBOUND -> inboundDocument(inboundService.detail(fid, tenantId));
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

    private BotpDocumentResponse orderDocument(PurchaseOrderDetail detail) {
        var order = detail.order();
        Map<String, Object> header = commonHeader(
                order.getFtenantId(), order.getForgId(), order.getFnumber(), order.getFdate(),
                order.getFbusinessPartnerId(), order.getFbusinessPartnerCode(), order.getFbusinessPartnerName(),
                order.getFcurrencyCode(), order.getFstatus(), order.getFapprovalStatus());
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
