package single.cjj.bizfi.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_ai_knowledge_base_acl")
public class BizfiAiKnowledgeBaseAcl implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "fid", type = IdType.AUTO)
    private Long fid;

    private String fkbid;
    private String fsubjecttype;
    private String fsubjectid;
    private String fpermission;
    private Long fcreatedby;
    private LocalDateTime fcreatetime;
    private LocalDateTime fmodifytime;
}
