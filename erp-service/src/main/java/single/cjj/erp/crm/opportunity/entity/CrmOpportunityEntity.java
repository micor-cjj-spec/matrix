package single.cjj.erp.crm.opportunity.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_crm_opportunity")
public class CrmOpportunityEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private String fnumber;
    private LocalDate fdate;
    private Long fleadId;
    private Long fbusinessPartnerId;
    private String fbusinessPartnerCode;
    private String fbusinessPartnerName;
    private String fname;
    private Long fownerId;
    private String fcurrencyCode;
    private BigDecimal fexpectedAmount;
    private LocalDate fexpectedCloseDate;
    private String fstage;
    private BigDecimal fprobability;
    private LocalDate fnextActionDate;
    private String fstatus;
    private String flostReason;
    private LocalDateTime fwonTime;
    private LocalDateTime flostTime;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
