package single.cjj.erp.sales.commercial.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@TableName("matrix_erp_sales_quote")
public class SalesQuoteEntity {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fid;
    private String ftenantId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long forgId;
    private String fnumber;
    private LocalDate fdate;
    private String fquoteType;
    private String ftenderReference;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fopportunityId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fbusinessPartnerId;
    private String fbusinessPartnerCode;
    private String fbusinessPartnerName;
    private String fcurrencyCode;
    private LocalDate fvalidUntil;
    private String fdeliveryTermCode;
    private String fpaymentTermCode;
    private BigDecimal fnetAmount;
    private BigDecimal ftaxAmount;
    private BigDecimal fgrossAmount;
    private String fstatus;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long facceptedBy;
    private LocalDateTime facceptedTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic private Integer fdeleteFlag;
    @Version private Integer fversion;
}
