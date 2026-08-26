package single.cjj.erp.procurement.order.entity;

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
@TableName("matrix_erp_purchase_order")
public class PurchaseOrderEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private String fnumber;
    private LocalDate fdate;
    private Long fbusinessPartnerId;
    private String fbusinessPartnerCode;
    private String fbusinessPartnerName;
    private Long fcontractId;
    private Long fcurrencyId;
    private String fcurrencyCode;
    private BigDecimal ftotalQuantity;
    private BigDecimal fnetAmount;
    private BigDecimal ftaxAmount;
    private BigDecimal fgrossAmount;
    private String fpaymentTermCode;
    private LocalDate fplannedDeliveryDate;
    private String fstatus;
    private String fapprovalStatus;
    private String freceiptStatus;
    private String finvoiceStatus;
    private String fsettlementStatus;
    private String fcloseStatus;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
