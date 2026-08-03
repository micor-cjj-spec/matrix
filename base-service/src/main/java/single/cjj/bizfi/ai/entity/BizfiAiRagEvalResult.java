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
@TableName("bizfi_ai_rag_eval_result")
public class BizfiAiRagEvalResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "fid", type = IdType.AUTO)
    private Long fid;
    private String frunid;
    private String fcaseid;
    private String fquestion;
    private String fexpecteddocids;
    private String fexpectedchunkids;
    private String fretrieveddocids;
    private String fretrievedchunkids;
    private Boolean fhit;
    private Integer ffirstrelevantrank;
    private Double freciprocalrank;
    private Double frecallatk;
    private Long flatencyms;
    private String ferrormessage;
    private LocalDateTime fcreatetime;
}
