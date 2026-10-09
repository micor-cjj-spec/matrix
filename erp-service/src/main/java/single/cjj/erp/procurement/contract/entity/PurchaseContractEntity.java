package single.cjj.erp.procurement.contract.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_purchase_contract")
public class PurchaseContractEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private String fnumber;
    private LocalDate fdate;
    private String ftitle;
    private Long fsourcingAwardId;
    private Long fbusinessPartnerId;
    private String fbusinessPartnerCode;
    private String fbusinessPartnerName;
    private String fcurrencyCode;
    private LocalDate fstartDate;
    private LocalDate fendDate;
    private String fpaymentTermCode;
    private String fdeliveryTermCode;
    private BigDecimal ftotalQuantity;
    private BigDecimal fnetAmount;
    private BigDecimal ftaxAmount;
    private BigDecimal fgrossAmount;
    private String fstatus;
    private String fapprovalStatus;
    private String fexecutionStatus;
    private String fworkflowInstanceId;
    private String frejectReason;
    private Long fapprovedBy;
    private LocalDateTime fapprovedTime;
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
