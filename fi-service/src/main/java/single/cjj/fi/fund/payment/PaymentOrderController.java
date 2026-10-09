package single.cjj.fi.fund.payment;

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
import single.cjj.fi.fund.payment.PaymentOrderContracts.ActionRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.ChannelFailureRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.CreateRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.Detail;
import single.cjj.fi.fund.payment.PaymentOrderContracts.LiquidityCheckRequest;
import single.cjj.fi.fund.payment.PaymentOrderContracts.SubmitToBankRequest;
import single.cjj.fi.integration.botp.BotpLifecycleClient;
import single.cjj.fi.integration.botp.BotpLifecycleContracts.TargetStatusEvent;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/fund/payment-orders")
public class PaymentOrderController {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrderController.class);

    private final PaymentOrderService service;
    private final BotpLifecycleClient lifecycleClient;

    public PaymentOrderController(
            PaymentOrderService service,
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

    @PostMapping("/{fid}/liquidity-check")
    public ApiResponse<Detail> liquidityCheck(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @Valid @RequestBody LiquidityCheckRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId
    ) {
        return ApiResponse.success(
                service.recordLiquidityCheck(fid, tenantId, request, operatorId));
    }

    @PostMapping("/{fid}/submit")
    public ApiResponse<Detail> submit(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestBody(required = false) ActionRequest request
    ) {
        return ApiResponse.success(service.submit(fid, tenantId, request));
    }

    @PostMapping("/{fid}/audit")
    public ApiResponse<Detail> audit(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @RequestBody(required = false) ActionRequest request
    ) {
        return ApiResponse.success(service.audit(fid, tenantId, request));
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
                    ? "付款单取消"
                    : request.reason();
            lifecycleClient.targetStatusEvent(new TargetStatusEvent(
                    "FI-PAYORD-CANCEL-" + fid + "-V" + cancelled.version(),
                    tenantId,
                    "MATRIX",
                    "FI_PAYMENT_ORDER",
                    "PAYORD:" + fid,
                    "CANCELLED",
                    reason,
                    operatorId == null ? "system" : String.valueOf(operatorId),
                    LocalDateTime.now()
            ));
        } catch (RuntimeException exception) {
            // Cancel is already committed. BOTP reconciliation compensates callback failure.
            log.warn("BOTP target status callback failed for payment order {}", fid, exception);
        }
        return ApiResponse.success(cancelled);
    }

    @PostMapping("/{fid}/submit-to-bank")
    public ApiResponse<Detail> submitToBank(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @Valid @RequestBody SubmitToBankRequest request
    ) {
        return ApiResponse.success(service.submitToBank(fid, tenantId, request));
    }

    @PostMapping("/{fid}/channel-failure")
    public ApiResponse<Detail> channelFailure(
            @PathVariable Long fid,
            @RequestParam String tenantId,
            @Valid @RequestBody ChannelFailureRequest request
    ) {
        return ApiResponse.success(service.markChannelFailed(fid, tenantId, request));
    }
}
