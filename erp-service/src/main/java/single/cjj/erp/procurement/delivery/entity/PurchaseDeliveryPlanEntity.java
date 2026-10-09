package single.cjj.erp.procurement.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_purchase_delivery_plan")
public class PurchaseDeliveryPlanEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private String fnumber;
    private LocalDate fdate;
    private Long fpurchaseOrderId;
    private String fpurchaseOrderNo;
    private Long fbusinessPartnerId;
    private String fbusinessPartnerCode;
    private String fbusinessPartnerName;
    private String fcurrencyCode;
    private String fstatus;
    private Long fcurrentResponseId;
    private LocalDateTime fpublishedTime;
    private LocalDateTime fconfirmedTime;
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
