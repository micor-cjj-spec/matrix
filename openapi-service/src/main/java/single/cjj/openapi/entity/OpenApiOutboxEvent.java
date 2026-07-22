package single.cjj.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_open_api_outbox_event")
public class OpenApiOutboxEvent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventId;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payloadJson;
    private String status;
    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime sentAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
