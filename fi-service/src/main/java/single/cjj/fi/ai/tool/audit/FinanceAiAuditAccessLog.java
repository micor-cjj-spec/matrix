package single.cjj.fi.ai.tool.audit;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bizfi_ai_audit_access_log")
public class FinanceAiAuditAccessLog {

    @TableId("fid")
    private Long fid;
    private String faccessrequestid;
    private String foperatorid;
    private String foperatorroles;
    private String faction;
    private String ffiltersummary;
    private String foutcome;
    private Long fresultcount;
    private Long fdurationms;
    private String ferrorcode;
    private LocalDateTime fcreatetime;
}
