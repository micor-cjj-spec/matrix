package single.cjj.bizfi.ai.entity;

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
@TableName("bizfi_ai_knowledge_doc")
public class BizfiAiKnowledgeDoc implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    private String fdocid;
    private String ftitle;
    private String fcategory;
    private String fsourcepath;
    private String fcontent;
    private String fversion;
    private String fstatus;
    private LocalDateTime fcreatetime;
    private LocalDateTime fmodifytime;
}
