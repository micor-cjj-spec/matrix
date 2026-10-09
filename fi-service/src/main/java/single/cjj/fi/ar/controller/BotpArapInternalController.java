package single.cjj.fi.ar.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.fi.ar.dto.BotpArapContracts.ArapWritebackRequest;
import single.cjj.fi.ar.dto.BotpArapContracts.PaymentApplicationCreateRequest;
import single.cjj.fi.ar.entity.BizfiFiArapDoc;
import single.cjj.fi.ar.service.BotpArapIntegrationService;
import single.cjj.fi.integration.botp.BotpLifecycleClient;
import single.cjj.fi.integration.botp.BotpLifecycleContracts.TargetStatusEvent;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/arap-doc/internal/botp")
public class BotpArapInternalController {

    private static final Logger log = LoggerFactory.getLogger(BotpArapInternalController.class);

    private final BotpArapIntegrationService service;
    private final BotpLifecycleClient lifecycleClient;

    public BotpArapInternalController(
            BotpArapIntegrationService service,
            BotpLifecycleClient lifecycleClient
    ) {
        this.service = service;
        this.lifecycleClient = lifecycleClient;
    }

    @GetMapping("/documents/{fid}")
    public ApiResponse<BizfiFiArapDoc> detail(@PathVariable("fid") Long fid) {
        return ApiResponse.success(service.detail(fid));
    }

    @GetMapping("/targets/by-idempotency")
    public ApiResponse<BizfiFiArapDoc> findByIdempotency(
            @RequestParam("key") String idempotencyKey
    ) {
        return ApiResponse.success(service.findByIdempotencyKey(idempotencyKey));
    }

    @PostMapping("/payment-applications")
    public ApiResponse<BizfiFiArapDoc> createPaymentApplication(
            @Valid @RequestBody PaymentApplicationCreateRequest request
    ) {
        return ApiResponse.success(service.createPaymentApplication(request));
    }

    @PostMapping("/payment-applications/{fid}/void")
    public ApiResponse<BizfiFiArapDoc> voidPaymentApplication(
            @PathVariable("fid") Long fid,
            @RequestParam(value = "operator", required = false) String operator,
            @RequestParam(value = "reason", defaultValue = "付款申请作废") String reason
    ) {
        BizfiFiArapDoc target = service.voidPaymentApplication(fid, operator);
        try {
            lifecycleClient.targetStatusEvent(new TargetStatusEvent(
                    "FI-PAYMENT-VOID-" + fid,
                    "default",
                    "MATRIX",
                    "FI_PAYMENT_APPLICATION",
                    String.valueOf(fid),
                    "VOID",
                    reason,
                    operator == null || operator.isBlank() ? "system" : operator,
                    LocalDateTime.now()
            ));
        } catch (RuntimeException exception) {
            // 作废事实已经提交，回调失败由 BOTP 定时对账补偿，不能回滚业务作废。
            log.warn("BOTP target status callback failed for payment application {}", fid, exception);
        }
        return ApiResponse.success(target);
    }

    @PostMapping("/documents/{fid}/writeback")
    public ApiResponse<BizfiFiArapDoc> recomputeWriteback(
            @PathVariable("fid") Long fid,
            @Valid @RequestBody ArapWritebackRequest request
    ) {
        return ApiResponse.success(service.recomputeWriteback(fid, request));
    }
}
