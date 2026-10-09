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
@TableName("bizfi_ai_evaluation_dataset")
public class BizfiAiEvaluationDataset implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "fid", type = IdType.AUTO)
    private Long fid;
    private String fdatasetid;
    private String fname;
    private String fdescription;
    private String fstatus;
    private LocalDateTime fcreatetime;
    private LocalDateTime fmodifytime;
}
