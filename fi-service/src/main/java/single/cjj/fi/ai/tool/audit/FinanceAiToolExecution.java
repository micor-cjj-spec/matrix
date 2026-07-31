package single.cjj.fi.ai.tool.audit;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bizfi_ai_tool_execution")
public class FinanceAiToolExecution {

    @TableId("fid")
    private Long fid;

    private String frequestid;
    private String ftoolname;
    private Long fuserid;
    private Long forganizationid;
    private String fperiod;
    private String fstatus;
    private Integer freadinessscore;
    private Integer fblockingcount;
    private Integer fwarningcount;
    private String fclosestatus;
    private Long fdurationms;
    private String ferrorcode;
    private String ferrormessage;
    private LocalDateTime fstarttime;
    private LocalDateTime fendtime;
    private LocalDateTime fcreatetime;
    private LocalDateTime fmodifytime;
}
