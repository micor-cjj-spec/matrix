package single.cjj.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import single.cjj.openapi.entity.OpenApiWriteRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherWriteStatusResponse {

    private String requestId;
    private String externalBizNo;
    private String status;
    private String tenantId;
    private String organizationId;
    private String bookId;
    private LocalDate voucherDate;
    private String summary;
    private Long voucherId;
    private String voucherNumber;
    private String errorCode;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finishedAt;

    public static VoucherWriteStatusResponse from(OpenApiWriteRequest request) {
        return new VoucherWriteStatusResponse(
                request.getRequestId(),
                request.getExternalBizNo(),
                request.getStatus(),
                request.getTenantId(),
                request.getOrganizationId(),
                request.getBookId(),
                request.getVoucherDate(),
                request.getSummary(),
                request.getVoucherId(),
                request.getVoucherNumber(),
                request.getErrorCode(),
                request.getErrorMessage(),
                request.getRetryCount(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getFinishedAt()
        );
    }
}
