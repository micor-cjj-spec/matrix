package single.cjj.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_open_api_write_request")
public class OpenApiWriteRequest {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private Long appId;
    private String appExternalId;
    private String tenantId;
    private String externalBizNo;
    private String idempotencyKey;
    private String requestBodyHash;
    private String organizationId;
    private String bookId;
    private LocalDate voucherDate;
    private String summary;
    private String status;
    private Long voucherId;
    private String voucherNumber;
    private String errorCode;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finishedAt;
}
