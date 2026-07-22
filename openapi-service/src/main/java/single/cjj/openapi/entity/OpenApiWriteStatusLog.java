package single.cjj.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_open_api_write_status_log")
public class OpenApiWriteStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long writeRequestId;
    private String fromStatus;
    private String toStatus;
    private String errorCode;
    private String message;
    private LocalDateTime createdAt;
}
