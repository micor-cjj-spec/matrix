package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSheetDrillResultVO {
    private Long templateId;
    private String templateName;
    private Long orgId;
    private String period;
    private String currency;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private List<BalanceSheetDrillRowVO> rows = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
