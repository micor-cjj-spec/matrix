package single.cjj.fi.gl.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportMappingGapVO {
    private String reportType;
    private Long templateId;
    private String templateName;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private String mappingType;
    private String actionLabel;
    private String targetRoute;
}
