package single.cjj.erp.sales.commercial.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@TableName("matrix_erp_sales_contract_entry")
public class SalesContractEntryEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long fcontractId;
    private Long fsourceQuoteEntryId;
    private Integer flineNo;
    private String fmaterialCode;
    private String fdescription;
    private BigDecimal fquantity;
    private BigDecimal funitPrice;
    private BigDecimal ftaxRate;
    private BigDecimal fnetAmount;
    private BigDecimal ftaxAmount;
    private BigDecimal fgrossAmount;
    private BigDecimal forderedQuantity;
    private BigDecimal fdeliveredQuantity;
    private BigDecimal finvoicedQuantity;
    private BigDecimal fsettledAmount;
    private LocalDateTime fcreateTime;
    @TableLogic private Integer fdeleteFlag;
}
