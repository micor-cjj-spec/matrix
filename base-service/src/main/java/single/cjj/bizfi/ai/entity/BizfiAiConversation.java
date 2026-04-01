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
@TableName("bizfi_ai_conversation")
public class BizfiAiConversation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    private String fconversationid;
    private Long fuserid;
    private String ftitle;
    private String fscene;
    private String fstatus;
    private String fsource;
    private LocalDateTime fcreatetime;
    private LocalDateTime fmodifytime;
}
