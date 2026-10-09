package single.cjj.erp.procurement.request.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_purchase_request")
public class PurchaseRequestEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private String fnumber;
    private LocalDate fdate;
    private Long frequesterId;
    private Long frequestDepartmentId;
    private String frequestType;
    private String fpurpose;
    private String fcurrencyCode;
    private BigDecimal fbudgetAmount;
    private BigDecimal ftotalQuantity;
    private BigDecimal festimatedAmount;
    private LocalDate frequiredDate;
    private Long fprojectId;
    private Long fcostCenterId;
    private String fsourceDocumentType;
    private String fsourceDocumentId;
    private String fsourceDocumentNo;
    private String fstatus;
    private String fapprovalStatus;
    private String fexecutionStatus;
    private String fworkflowInstanceId;
    private String frejectReason;
    private Long fapprovedBy;
    private LocalDateTime fapprovedTime;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
