package single.cjj.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_open_api_callback_task")
public class OpenApiCallbackTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventId;
    private Long writeRequestId;
    private String requestId;
    private Long appId;
    private String callbackUrl;
    private String eventType;
    private String payloadJson;
    private String status;
    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime nextAttemptAt;
    private Integer lastHttpStatus;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
