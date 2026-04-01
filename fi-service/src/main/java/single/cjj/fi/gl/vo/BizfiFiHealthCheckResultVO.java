package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BizfiFiHealthCheckResultVO {
    private Long forg;
    private Long templateId;
    private LocalDateTime checkedAt;
    private Boolean healthy;
    private Integer totalIssueCount;
    private Integer issueTypeCount;
    private List<String> notes;
    private List<BizfiFiHealthCheckIssueVO> issues;
}
