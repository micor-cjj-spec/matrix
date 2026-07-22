package single.cjj.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("matrix_open_api_write_request_line")
public class OpenApiWriteRequestLine {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long writeRequestId;
    private Integer lineNo;
    private String accountCode;
    private String summary;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String currency;
    private BigDecimal rate;
    private BigDecimal originalAmount;
    private String cashflowItem;
}
