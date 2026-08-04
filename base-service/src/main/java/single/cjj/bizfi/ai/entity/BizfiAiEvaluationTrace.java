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
@TableName("bizfi_ai_evaluation_trace")
public class BizfiAiEvaluationTrace implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "fid", type = IdType.AUTO)
    private Long fid;
    private String ftraceid;
    private String frunid;
    private String fresultid;
    private String fquestionid;
    private String fconfigfingerprint;
    private String fmode;
    private String ftracejson;
    private LocalDateTime fcreatetime;
}
