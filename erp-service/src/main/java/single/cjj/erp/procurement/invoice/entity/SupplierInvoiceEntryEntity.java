package single.cjj.erp.procurement.invoice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("matrix_erp_supplier_invoice_entry")
public class SupplierInvoiceEntryEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long forgId;
    private Long fsupplierInvoiceId;
    private Integer flineNo;
    private Long fpurchaseOrderId;
    private Long fpurchaseOrderEntryId;
    private Long fmaterialId;
    private String fmaterialCode;
    private String fmaterialName;
    private String fspecification;
    private BigDecimal fquantity;
    private BigDecimal funitPrice;
    private BigDecimal fnetAmount;
    private BigDecimal ftaxRate;
    private BigDecimal ftaxAmount;
    private BigDecimal fgrossAmount;
    private String fmatchStatus;
    private Long freconciliationCaseId;
    private BigDecimal fmatchedInboundQuantity;
    private String fdifferenceCodes;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
