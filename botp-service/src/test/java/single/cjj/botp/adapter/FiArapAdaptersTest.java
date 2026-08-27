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
import single.cjj.botp.integration.fi.FiPaymentApplicationClient;
import single.cjj.botp.integration.fi.FiPaymentApplicationClientContracts.BotpCreateRequest;
import single.cjj.botp.integration.fi.FiPaymentApplicationClientContracts.BotpDocument;
import single.cjj.botp.integration.fi.FiPaymentApplicationClientContracts.PayableSnapshot;
import single.cjj.botp.integration.fi.FiPaymentApplicationClientContracts.PaymentApplicationDetail;

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
        FiPaymentApplicationAdapter targetAdapter = new FiPaymentApplicationAdapter(
                client, new FakeCanonicalClient());
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

    @Test
    void shouldUseCanonicalFormalPayableForNewP2pChain() {
        FakeFiArapClient legacy = new FakeFiArapClient();
        FakeCanonicalClient canonical = new FakeCanonicalClient();
        FiFormalPayableDocumentAdapter sourceAdapter = new FiFormalPayableDocumentAdapter(canonical);
        FiPaymentApplicationAdapter targetAdapter = new FiPaymentApplicationAdapter(legacy, canonical);

        DocumentRef sourceRef = new DocumentRef("MATRIX", "FI_AP_PAYABLE", "10", List.of());
        DocumentData source = sourceAdapter.load(sourceRef);
        sourceAdapter.validateSource(source, Map.of(
                "tenantId", "T1",
                "pushAmount", new BigDecimal("600")
        ));
        assertThrows(BizException.class, () -> sourceAdapter.validateSource(source, Map.of(
                "tenantId", "T1",
                "pushAmount", new BigDecimal("901")
        )));

        TargetDraft draft = new TargetDraft(
                "MATRIX",
                "FI_PAYMENT_APPLICATION",
                Map.of(
                        "tenantId", "T1",
                        "orgId", 1L,
                        "sourceSystem", "MATRIX",
                        "sourceDocumentType", "FI_AP_PAYABLE",
                        "sourceDocumentId", "10",
                        "sourceExecutionId", "BOTP-NEW-1",
                        "sourceBillNo", "AP-FORMAL-001",
                        "counterparty", "BP-1",
                        "amount", new BigDecimal("600"),
                        "payMethod", "BANK_DIRECT",
                        "plannedPayDate", "2026-08-30",
                        "operatorId", 99L
                ),
                List.of()
        );

        TargetResult target = targetAdapter.createTarget(draft, "botp:T1:BOTP-NEW-1:0");

        assertEquals("PA:20", target.documentId());
        assertEquals(10L, canonical.createRequest.payableId());
        assertEquals("T1", canonical.createRequest.tenantId());
        assertEquals(new BigDecimal("600"), canonical.createRequest.amount());

        sourceAdapter.applyWriteback(new WritebackCommand(
                "BOTP-NEW-1",
                sourceRef,
                target,
                List.of(),
                Map.of("tenantId", "T1", "operatorId", 99L)
        ));
        assertEquals(10L, canonical.recomputePayableId);
        assertEquals("T1", canonical.recomputeTenantId);
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
    private static class FakeCanonicalClient implements FiPaymentApplicationClient {

        private BotpCreateRequest createRequest;
        private Long recomputePayableId;
        private String recomputeTenantId;

        @Override
        public ApiResponse<BotpDocument> payable(Long fid) {
            return ApiResponse.success(new BotpDocument(
                    "AP:10", 10L, "AP-FORMAL-001", LocalDate.of(2026, 8, 27),
                    "T1", 1L, "BP-1", "SUP-1", "供应商1", "CNY",
                    new BigDecimal("1000"), new BigDecimal("1000"), new BigDecimal("100"),
                    new BigDecimal("900"), "OPEN", "AUDITED", "VOUCHER_GENERATED",
                    null, null, null, null, null, null, 0
            ));
        }

        @Override
        public ApiResponse<BotpDocument> application(Long fid) {
            return ApiResponse.success(new BotpDocument(
                    "PA:20", 20L, "PAYAPP-001", LocalDate.of(2026, 8, 27),
                    "T1", 1L, "BP-1", "SUP-1", "供应商1", "CNY",
                    new BigDecimal("600"), null, null, null,
                    "DRAFT", "DRAFT", null, "BANK_DIRECT", LocalDate.of(2026, 8, 30),
                    "FI_AP_PAYABLE", "10", "BOTP-NEW-1", "botp:T1:BOTP-NEW-1:0", 0
            ));
        }

        @Override
        public ApiResponse<PaymentApplicationDetail> findByIdempotency(
                String tenantId, String key) {
            return ApiResponse.success(null);
        }

        @Override
        public ApiResponse<PaymentApplicationDetail> create(BotpCreateRequest request) {
            this.createRequest = request;
            return ApiResponse.success(new PaymentApplicationDetail(
                    20L, request.tenantId(), request.orgId(), "PAYAPP-001",
                    LocalDate.of(2026, 8, 27), "BP-1", "SUP-1", "供应商1", "CNY",
                    request.amount(), request.plannedPayDate(), request.payMethod(),
                    "DRAFT", "DRAFT", "NOT_EXECUTED", request.idempotencyKey(),
                    request.sourceDocumentType(), request.sourceDocumentId(),
                    request.sourceExecutionId(), 0
            ));
        }

        @Override
        public ApiResponse<PayableSnapshot> recomputeReservation(
                Long fid, String tenantId, Long operatorId) {
            this.recomputePayableId = fid;
            this.recomputeTenantId = tenantId;
            return ApiResponse.success(new PayableSnapshot(
                    10L, "T1", 1L, "AP-FORMAL-001", "FORMAL",
                    LocalDate.of(2026, 8, 27), "BP-1", "SUP-1", "供应商1",
                    "CNY", new BigDecimal("1000"), new BigDecimal("1000"),
                    new BigDecimal("600"), new BigDecimal("400"),
                    "OPEN", "AUDITED", "VOUCHER_GENERATED", 1
            ));
        }
    }
}
