package single.cjj.fi.notice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_internal_notice")
public class BizfiFiInternalNotice implements Serializable {
    @TableId("fid")
    private Long fid;
    private String fnoticeType;
    private String fdocTypeRoot;
    private Long forgId;
    private String fperiod;
    private String fstatus;
    private String fseverity;
    private String freferenceKey;
    private String freferenceType;
    private String fsourceCode;
    private String fcategoryCode;
    private String fcounterparty;
    private Long fvoucherId;
    private String fvoucherNumber;
    private String ftitle;
    private String fmessage;
    private String fsuggestion;
    private BigDecimal famount;
    private BigDecimal fopenAmount;
    private LocalDate fsourceDate;
    private LocalDate fdueDate;
    private String fowner;
    private LocalDateTime fnoticeTime;
    private LocalDateTime fupdateTime;
    private LocalDateTime fresolvedTime;
    private String fresolveNote;
}
