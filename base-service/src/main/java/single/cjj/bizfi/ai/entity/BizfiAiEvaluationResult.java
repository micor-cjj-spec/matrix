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
@TableName("bizfi_ai_evaluation_result")
public class BizfiAiEvaluationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "fid", type = IdType.AUTO)
    private Long fid;
    private String fresultid;
    private String frunid;
    private String fquestionid;
    private String fcitationsjson;
    private Integer ffirstrelevantrank;
    private Double frecall;
    private Double freciprocalrank;
    private Long flatencyms;
    private String ferrormessage;
    private LocalDateTime fcreatetime;
}
