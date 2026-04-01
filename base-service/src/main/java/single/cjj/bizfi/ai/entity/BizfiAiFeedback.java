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
@TableName("bizfi_ai_feedback")
public class BizfiAiFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    private String fconversationid;
    private Long fmessageid;
    private Long fuserid;
    private String ffeedbacktype;
    private String fcomment;
    private LocalDateTime fcreatetime;
}
