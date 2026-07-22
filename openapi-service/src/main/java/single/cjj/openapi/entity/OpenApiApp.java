package single.cjj.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_open_api_app")
public class OpenApiApp {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String appId;
    private String appName;
    private String appKey;
    private String appSecretCipher;
    private String tenantId;
    private String status;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String ipWhitelist;
    private Integer qpsLimit;
    private Integer maxPageSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
