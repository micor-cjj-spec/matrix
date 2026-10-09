package single.cjj.botp.integration.fi;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class FiArapClientContracts {

    private FiArapClientContracts() {
    }

    public record FiArapDocument(
            Long fid,
            String fdoctype,
            String fnumber,
            LocalDate fdate,
            String fcounterparty,
            BigDecimal famount,
            String fstatus,
            String fremark,
            String fpayMethod,
            LocalDate fplannedPayDate,
            String fsourceBillNo,
            BigDecimal fappliedAmount,
            BigDecimal freservedAmount,
            BigDecimal fremainingAmount,
            String fpushStatus,
            String fbotpIdempotencyKey,
            String fsourceSystem,
            String fsourceDocumentType,
            String fsourceDocumentId,
            String fsourceExecutionId,
            Integer fversion
    ) {
    }

    public record PaymentApplicationCreateRequest(
            String idempotencyKey,
            String sourceSystem,
            String sourceDocumentType,
            String sourceDocumentId,
            String sourceExecutionId,
            String sourceBillNo,
            String counterparty,
            BigDecimal amount,
            String payMethod,
            LocalDate plannedPayDate,
            String operator
    ) {
    }

    public record ArapWritebackRequest(
            BigDecimal activeAllocatedAmount,
            BigDecimal releaseReservedAmount,
            String executionId
    ) {
    }
}
