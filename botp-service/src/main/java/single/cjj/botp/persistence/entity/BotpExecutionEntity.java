package single.cjj.botp.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_botp_execution")
public class BotpExecutionEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private String fexecutionId;
    private String fsourceSystem;
    private String frequestId;
    private String fruleCode;
    private Integer fruleVersion;
    private String fexecutionMode;
    private String fstatus;
    private String frequestJson;
    private String ferrorMessage;
    private Integer fretryCount;
    private LocalDateTime fstartTime;
    private LocalDateTime ffinishTime;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
