package single.cjj.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_open_api_request_log")
public class OpenApiRequestLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private String appId;
    private String apiCode;
    private String apiVersion;
    private String httpMethod;
    private String requestPath;
    private String clientIp;
    private String responseCode;
    private Integer httpStatus;
    private Boolean success;
    private Long durationMs;
    private LocalDateTime requestTime;
    private LocalDateTime responseTime;
    private String errorMessage;
}
