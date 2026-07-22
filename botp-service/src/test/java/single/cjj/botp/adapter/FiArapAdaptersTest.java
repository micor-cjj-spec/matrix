package single.cjj.botp.adapter;

import org.junit.jupiter.api.Test;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.bizfi.exception.BizException;
import single.cjj.botp.domain.BotpContracts.DocumentData;
import single.cjj.botp.domain.BotpContracts.DocumentRef;
import single.cjj.botp.domain.BotpContracts.TargetDraft;
import single.cjj.botp.domain.BotpContracts.TargetResult;
import single.cjj.botp.domain.BotpContracts.WritebackCommand;
import single.cjj.botp.integration.fi.FiArapClient;
import single.cjj.botp.integration.fi.FiArapClientContracts.ArapWritebackRequest;
import single.cjj.botp.integration.fi.FiArapClientContracts.FiArapDocument;
import single.cjj.botp.integration.fi.FiArapClientContracts.PaymentApplicationCreateRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FiArapAdaptersTest {

    @Test
    void shouldValidateCreateAndWriteBackPartialAmount() {
        FakeFiArapClient client = new FakeFiArapClient();
        FiPayableDocumentAdapter sourceAdapter = new FiPayableDocumentAdapter(client);
        FiPaymentApplicationAdapter targetAdapter = new FiPaymentApplicationAdapter(client);
        DocumentRef sourceRef = new DocumentRef("MATRIX", "FI_AP_DOC", "1", List.of());

        DocumentData source = sourceAdapter.load(sourceRef);
        sourceAdapter.validateSource(source, Map.of("pushAmount", new BigDecimal("600")));
        assertThrows(
                BizException.class,
                () -> sourceAdapter.validateSource(source, Map.of("pushAmount", new BigDecimal("901")))
        );

        TargetDraft draft = new TargetDraft(
                "MATRIX",
                "FI_PAYMENT_APPLICATION",
                Map.of(
                        "sourceSystem", "MATRIX",
                        "sourceDocumentType", "FI_AP_DOC",
                        "sourceDocumentId", "1",
                        "sourceExecutionId", "BOTP-1",
                        "sourceBillNo", "AP-001",
                        "counterparty", "SUPPLIER-1",
                        "amount", new BigDecimal("600"),
                        "payMethod", "BANK",
                        "plannedPayDate", "2026-07-30",
                        "operator", "admin"
                ),
                List.of()
        );
        TargetResult target = targetAdapter.createTarget(draft, "botp:default:BOTP-1:0");

        assertEquals("2", target.documentId());
        assertEquals(new BigDecimal("600"), client.createRequest.amount());
        assertEquals("1", client.createRequest.sourceDocumentId());

        sourceAdapter.applyWriteback(new WritebackCommand(
                "BOTP-1",
                sourceRef,
                target,
                List.of(),
                Map.of(
                        "activeAllocatedAmount", new BigDecimal("600"),
                        "releaseReservedAmount", new BigDecimal("600")
                )
        ));
        assertEquals(new BigDecimal("600"), client.writebackRequest.activeAllocatedAmount());
        assertEquals(new BigDecimal("600"), client.writebackRequest.releaseReservedAmount());
    }

    private static class FakeFiArapClient implements FiArapClient {

        private PaymentApplicationCreateRequest createRequest;
        private ArapWritebackRequest writebackRequest;

        @Override
        public ApiResponse<FiArapDocument> detail(Long fid) {
            return ApiResponse.success(new FiArapDocument(
                    1L,
                    "AP",
                    "AP-001",
                    LocalDate.of(2026, 7, 22),
                    "SUPPLIER-1",
                    new BigDecimal("1000"),
                    "AUDITED",
                    null,
                    null,
                    null,
                    null,
                    new BigDecimal("100"),
                    BigDecimal.ZERO,
                    new BigDecimal("900"),
                    "PARTIAL",
                    null,
                    null,
                    null,
                    null,
                    null,
                    0
            ));
        }

        @Override
        public ApiResponse<FiArapDocument> findByIdempotency(String idempotencyKey) {
            return ApiResponse.success(null);
        }

        @Override
        public ApiResponse<FiArapDocument> createPaymentApplication(PaymentApplicationCreateRequest request) {
            this.createRequest = request;
            return ApiResponse.success(new FiArapDocument(
                    2L,
                    "AP_PAYMENT_APPLY",
                    "AP_PAYMENT_APPLY-001",
                    LocalDate.of(2026, 7, 22),
                    request.counterparty(),
                    request.amount(),
                    "DRAFT",
                    null,
                    request.payMethod(),
                    request.plannedPayDate(),
                    request.sourceBillNo(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    request.amount(),
                    "NOT_PUSHED",
                    request.idempotencyKey(),
                    request.sourceSystem(),
                    request.sourceDocumentType(),
                    request.sourceDocumentId(),
                    request.sourceExecutionId(),
                    0
            ));
        }

        @Override
        public ApiResponse<FiArapDocument> recomputeWriteback(Long fid, ArapWritebackRequest request) {
            this.writebackRequest = request;
            return detail(fid);
        }
    }
}
