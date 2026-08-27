package single.cjj.fi.ap.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PaymentSettlementContracts {

    private PaymentSettlementContracts() {
    }

    public record FinalizeRequest(
            Long operatorId
    ) {
    }

    public record EntryView(
            Long fid,
            Long payableId,
            String payableNumber,
            Long paymentApplicationId,
            Long paymentApplicationAllocationId,
            Long paymentOrderAllocationId,
            BigDecimal settledAmount,
            BigDecimal originalOpenAmount,
            BigDecimal remainingOpenAmount,
            BigDecimal originalReservedAmount,
            BigDecimal remainingReservedAmount,
            String status
    ) {
    }

    public record Detail(
            Long fid,
            String tenantId,
            Long orgId,
            String number,
            Long paymentOrderId,
            Long bankTransactionId,
            String businessPartnerId,
            String businessPartnerCode,
            String businessPartnerName,
            String currencyCode,
            BigDecimal amount,
            String status,
            LocalDate settlementDate,
            String businessEventId,
            String accountingEventId,
            Long voucherId,
            String voucherNumber,
            LocalDateTime createTime,
            List<EntryView> entries
    ) {
    }
}
