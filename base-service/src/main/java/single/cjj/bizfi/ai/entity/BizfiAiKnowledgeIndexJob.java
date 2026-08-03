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
@TableName("bizfi_ai_knowledge_index_job")
public class BizfiAiKnowledgeIndexJob implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "fid", type = IdType.AUTO)
    private Long fid;
    private String fjobid;
    private String fkbid;
    private String fdocid;
    private String ffilename;
    private String fmediatype;
    private Long ffilesize;
    private String fcontenthash;
    private String fstatus;
    private Integer fattempts;
    private Integer fmaxattempts;
    private String ferrormessage;
    private LocalDateTime fnextretrytime;
    private LocalDateTime fstarttime;
    private LocalDateTime ffinishtime;
    private LocalDateTime fcreatetime;
    private LocalDateTime fmodifytime;
}
