package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import single.cjj.fi.gl.entity.BizfiFiVoucher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodProcessResultVO {
    private String moduleCode;
    private String moduleName;
    private Long forg;
    private String period;
    private String periodSource;
    private String baseCurrency;
    private String currentPeriod;
    private String periodStatus;
    private String defaultVoucherType;
    private Boolean foundationHealthy;
    private Integer periodVoucherCount;
    private Integer matchedVoucherCount;
    private Integer postedVoucherCount;
    private Integer pendingVoucherCount;
    private Integer exceptionVoucherCount;
    private BigDecimal matchedAmount;
    private String moduleStatus;
    private List<VoucherCarryTaskVO> tasks = new ArrayList<>();
    private List<BizfiFiVoucher> relatedVouchers = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
