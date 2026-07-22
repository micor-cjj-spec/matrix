package single.cjj.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_open_api_definition")
public class OpenApiDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String apiCode;
    private String apiName;
    private String apiVersion;
    private String httpMethod;
    private String externalPath;
    private String scopeCode;
    private String status;
    private Integer maxPageSize;
    private String sensitivityLevel;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
