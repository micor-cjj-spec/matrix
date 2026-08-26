package single.cjj.erp.procurement.acceptance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_purchase_acceptance")
public class PurchaseAcceptanceEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private String fnumber;
    private LocalDate fdate;
    private Long fpurchaseReceiptId;
    private Long fbusinessPartnerId;
    private String fbusinessPartnerCode;
    private String fbusinessPartnerName;
    private String fcurrencyCode;
    private String fstatus;
    private String fapprovalStatus;
    private String fresult;
    private String fbotpIdempotencyKey;
    private String fsourceExecutionId;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
