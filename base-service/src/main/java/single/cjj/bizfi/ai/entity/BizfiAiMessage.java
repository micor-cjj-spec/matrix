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
@TableName("bizfi_ai_message")
public class BizfiAiMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    private String fconversationid;
    private String frole;
    private String fcontent;
    private String fmodel;
    private String fmode;
    private String ftraceid;
    private Integer fprompttokens;
    private Integer fcompletiontokens;
    private Integer ftotaltokens;
    private LocalDateTime fcreatetime;
}
