package single.cjj.erp.crm.lead.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_crm_lead")
public class CrmLeadEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private String fnumber;
    private LocalDate fdate;
    private String fname;
    private String fcompanyName;
    private String fcontactName;
    private String fcontactPhone;
    private String fcontactEmail;
    private String fsource;
    private Long fownerId;
    private BigDecimal festimatedAmount;
    private String fcurrencyCode;
    private LocalDate fnextActionDate;
    private String fstatus;
    private LocalDateTime fqualifiedTime;
    private String fdisqualifiedReason;
    private Long fconvertedBusinessPartnerId;
    private Long fconvertedOpportunityId;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
