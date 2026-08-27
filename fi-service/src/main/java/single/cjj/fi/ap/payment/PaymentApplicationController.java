package single.cjj.fi.ap.payment;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.ActionRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.BudgetCheckRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.CreateRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.Detail;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.EvidenceRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.EvidenceVerifyRequest;
import single.cjj.fi.ap.payment.PaymentApplicationContracts.PayableSnapshot;
import single.cjj.fi.integration.botp.BotpLifecycleClient;
import single.cjj.fi.integration.botp.BotpLifecycleContracts.TargetStatusEvent;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/ap/payment-applications")
public class PaymentApplicationController {

    private static final Logger log = LoggerFactory.getLogger(PaymentApplicationController.class);

    private final PaymentApplicationService service;
    private final BotpLifecycleClient lifecycleClient;

    public PaymentApplicationController(
            PaymentApplicationService service,
            BotpLifecycleClient lifecycleClient
    ) {
        this.service = service;
        this.lifecycleClient = lifecycleClient;
    }

    @PostMapping
    public ApiResponse<Detail> create(
            @Valid @RequestBody CreateRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.create(request, operatorId));
    }

    @GetMapping("/{fid}")
    public ApiResponse<Detail> detail(
            @PathVariable Long fid,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.detail(fid, tenantId));
    }

    @GetMapping
    public ApiResponse<List<Detail>> list(
            @RequestParam String tenantId,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ApiResponse.success(service.list(tenantId, orgId, status, limit));
    }

    @GetMapping("/payables/{payableId}")
    public ApiResponse<PayableSnapshot> payable(
            @PathVariable Long payableId,
            @RequestParam String tenantId
    ) {
        return ApiResponse.success(service.payable(payableId, tenantId));
    }

    @PostMapping("/{fid}/evidence")
    public ApiResponse<Detail> addEvidence(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @Valid @RequestBody EvidenceRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(service.addEvidence(fid, tenantId, request, operatorId));
    }

    @PostMapping("/{fid}/evidence/{evidenceId}/verify")
    public ApiResponse<Detail> verifyEvidence(
            @PathVariable Long fid,
            @PathVariable Long evidenceId,
            @RequestParam String tenantId,
            @Valid @RequestBody EvidenceVerifyRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(
                service.verifyEvidence(fid, evidenceId, tenantId, request, operatorId));
    }

    @PostMapping("/{fid}/budget-check")
    public ApiResponse<Detail> budgetCheck(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @Valid @RequestBody BudgetCheckRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(
                service.recordBudgetCheck(fid, tenantId, request, operatorId));
    }

    @PostMapping("/{fid}/submit")
    public ApiResponse<Detail> submit(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestBody(required = false) ActionRequest request
    ) {
        return ApiResponse.success(service.submit(fid, tenantId, request));
    }

    @PostMapping("/{fid}/approve")
    public ApiResponse<Detail> approve(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestBody(required = false) ActionRequest request
    ) {
        return ApiResponse.success(service.approve(fid, tenantId, request));
    }

    @PostMapping("/{fid}/reject")
    public ApiResponse<Detail> reject(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestBody(required = false) ActionRequest request
    ) {
        return ApiResponse.success(service.reject(fid, tenantId, request));
    }

    @PostMapping("/{fid}/cancel")
    public ApiResponse<Detail> cancel(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestBody(required = false) ActionRequest request
    ) {
        Detail cancelled = service.cancel(fid, tenantId, request);
        try {
            Long operatorId = request == null ? null : request.operatorId();
            String reason = request == null || request.reason() == null || request.reason().isBlank()
                    ? "付款申请取消"
                    : request.reason();
            lifecycleClient.targetStatusEvent(new TargetStatusEvent(
                    "FI-PAYAPP-CANCEL-" + fid + "-V" + cancelled.version(),
                    tenantId,
                    "MATRIX",
                    "FI_PAYMENT_APPLICATION",
                    "PA:" + fid,
                    "CANCELLED",
                    reason,
                    operatorId == null ? "system" : String.valueOf(operatorId),
                    LocalDateTime.now()
            ));
        } catch (RuntimeException exception) {
            // Cancel is already committed. BOTP reconciliation is responsible for compensation.
            log.warn("BOTP target status callback failed for payment application {}", fid, exception);
        }
        return ApiResponse.success(cancelled);
    }
}
