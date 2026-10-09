package single.cjj.erp.sales.commercial.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@TableName("matrix_erp_sales_quote_entry")
public class SalesQuoteEntryEntity {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fid;
    private String ftenantId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fquoteId;
    private Integer flineNo;
    private String fmaterialCode;
    private String fdescription;
    private BigDecimal fquantity;
    private BigDecimal funitPrice;
    private BigDecimal ftaxRate;
    private BigDecimal fnetAmount;
    private BigDecimal ftaxAmount;
    private BigDecimal fgrossAmount;
    private LocalDateTime fcreateTime;
    @TableLogic private Integer fdeleteFlag;
}
