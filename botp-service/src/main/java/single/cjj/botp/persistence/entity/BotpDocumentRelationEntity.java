package single.cjj.botp.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("matrix_botp_document_relation")
public class BotpDocumentRelationEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private String fexecutionId;
    private String fruleCode;
    private Integer fruleVersion;
    private String fsourceSystemCode;
    private String fsourceDocumentType;
    private String fsourceDocumentId;
    private String ftargetSystemCode;
    private String ftargetDocumentType;
    private String ftargetDocumentId;
    private String ftargetDocumentNo;
    private BigDecimal fallocatedAmount;
    private String frelationStatus;
    private String ftargetStatus;
    private String flastEventId;
    private String finvalidReason;
    private LocalDateTime finvalidTime;
    private LocalDateTime freversedTime;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
