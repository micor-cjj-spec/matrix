package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BizfiFiHealthCheckIssueVO {
    private String code;
    private String name;
    private String severity;
    private Integer count;
    private List<String> samples;
    private String suggestion;
}
