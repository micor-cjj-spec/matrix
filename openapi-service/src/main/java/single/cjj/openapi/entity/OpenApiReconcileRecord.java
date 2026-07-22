package single.cjj.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_open_api_reconcile_record")
public class OpenApiReconcileRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordId;
    private String issueKey;
    private String issueType;
    private String severity;
    private Long writeRequestId;
    private String requestId;
    private Long appId;
    private String expectedStatus;
    private String actualStatus;
    private String detailMessage;
    private String status;
    private String resolution;
    private String resolvedBy;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
