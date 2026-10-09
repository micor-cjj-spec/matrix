package single.cjj.erp.procurement.invoice.entity;

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
@TableName("matrix_erp_supplier_invoice")
public class SupplierInvoiceEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private String fnumber;
    private String finvoiceNo;
    private String finvoiceCode;
    private LocalDate finvoiceDate;
    private Long fbusinessPartnerId;
    private String fbusinessPartnerCode;
    private String fbusinessPartnerName;
    private String fcurrencyCode;
    private BigDecimal fnetAmount;
    private BigDecimal ftaxAmount;
    private BigDecimal fgrossAmount;
    private String fstatus;
    private String fapprovalStatus;
    private String fmatchStatus;
    private String faccountingStatus;
    private String fmatchRequestId;
    private Long freconciliationBatchId;
    private String fmatchSummaryJson;
    private LocalDateTime fmatchTime;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
