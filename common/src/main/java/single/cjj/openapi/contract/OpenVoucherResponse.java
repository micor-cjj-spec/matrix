package single.cjj.openapi.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenVoucherResponse {

    private String voucherId;
    private String voucherNumber;
    private LocalDate voucherDate;
    private String summary;
    private BigDecimal amount;
    private String status;
    private String createdBy;
    private LocalDateTime createdTime;
    private String auditedBy;
    private LocalDateTime auditedTime;
    private String postedBy;
    private LocalDateTime postedTime;
}
