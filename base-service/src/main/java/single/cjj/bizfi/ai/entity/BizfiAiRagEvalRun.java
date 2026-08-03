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
@TableName("bizfi_ai_rag_eval_run")
public class BizfiAiRagEvalRun implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "fid", type = IdType.AUTO)
    private Long fid;
    private String frunid;
    private String fsetid;
    private String fkbid;
    private String fstatus;
    private Integer fcasecount;
    private Integer fcompletedcount;
    private Integer fhitcount;
    private Double fhitatk;
    private Double fmrr;
    private Double frecallatk;
    private Double favglatencyms;
    private Long fp95latencyms;
    private String fconfigsnapshot;
    private String ferrormessage;
    private Long fcreatedby;
    private LocalDateTime fstarttime;
    private LocalDateTime ffinishtime;
    private LocalDateTime fcreatetime;
    private LocalDateTime fmodifytime;
}
