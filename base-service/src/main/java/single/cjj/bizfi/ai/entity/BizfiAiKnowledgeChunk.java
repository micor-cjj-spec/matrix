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
@TableName("bizfi_ai_knowledge_chunk")
public class BizfiAiKnowledgeChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    private String fdocid;
    private String fchunkid;
    private Integer fseq;
    private String fcontent;
    private String fkeywords;
    private LocalDateTime fcreatetime;
}
