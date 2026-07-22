package single.cjj.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_open_api_grant")
public class OpenApiGrant {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long appId;
    private Long apiDefinitionId;
    private String status;
    private String dataPermissionJson;
    private String fieldPermissionJson;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
