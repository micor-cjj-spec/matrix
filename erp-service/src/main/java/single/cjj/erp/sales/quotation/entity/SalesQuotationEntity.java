package single.cjj.erp.sales.quotation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_sales_quotation")
public class SalesQuotationEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private String fnumber;
    private LocalDate fdate;
    private Long fopportunityId;
    private Long fbusinessPartnerId;
    private String fbusinessPartnerCode;
    private String fbusinessPartnerName;
    private String fquotationType;
    private String ftitle;
    private String fcurrencyCode;
    private LocalDate fvalidUntil;
    private String fpaymentTermCode;
    private String fdeliveryTermCode;
    private BigDecimal ftotalQuantity;
    private BigDecimal fnetAmount;
    private BigDecimal ftaxAmount;
    private BigDecimal fgrossAmount;
    private String fstatus;
    private String fapprovalStatus;
    private String fcustomerDecisionStatus;
    private String fworkflowInstanceId;
    private String frejectReason;
    private String fcustomerRejectReason;
    private LocalDateTime fsentTime;
    private LocalDateTime facceptedTime;
    private String fremark;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
