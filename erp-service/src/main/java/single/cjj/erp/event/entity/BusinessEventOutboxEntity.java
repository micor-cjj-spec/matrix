package single.cjj.erp.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_business_event_outbox")
public class BusinessEventOutboxEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private String feventId;
    private String feventType;
    private Integer feventVersion;
    private String fproducerService;
    private String fdomainCode;
    private String faggregateType;
    private String faggregateId;
    private Long faggregateVersion;
    private String fsourceSystemCode;
    private String fsourceDocumentType;
    private String fsourceDocumentId;
    private String fsourceDocumentNo;
    private LocalDate fbusinessDate;
    private String fcorrelationId;
    private String fcausationId;
    private String ftraceId;
    private Long foperatorId;
    private String froutingKey;
    private String fpayloadJson;
    private String fstatus;
    private Integer fretryCount;
    private Integer fmaxRetry;
    private LocalDateTime fnextRetryTime;
    private String fclaimToken;
    private LocalDateTime fclaimTime;
    private LocalDateTime fsentTime;
    private String flastError;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
