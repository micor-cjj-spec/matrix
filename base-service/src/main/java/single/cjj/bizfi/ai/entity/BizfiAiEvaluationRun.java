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
@TableName("bizfi_ai_evaluation_run")
public class BizfiAiEvaluationRun implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "fid", type = IdType.AUTO)
    private Long fid;
    private String frunid;
    private String fdatasetid;
    private String fstatus;
    private Integer ftopk;
    private Integer ftotalquestions;
    private Integer fcompletedquestions;
    private Double frecallatk;
    private Double fmrr;
    private Double fzerohitrate;
    private Long favglatencyms;
    private String ferrormessage;
    private LocalDateTime fstarttime;
    private LocalDateTime ffinishtime;
    private LocalDateTime fcreatetime;
    private LocalDateTime fmodifytime;
}
