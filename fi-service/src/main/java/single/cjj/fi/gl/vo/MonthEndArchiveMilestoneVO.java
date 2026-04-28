package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthEndArchiveMilestoneVO {
    private String code;
    private String name;
    private String status;
    private LocalDateTime time;
    private String operator;
    private String summary;
}
