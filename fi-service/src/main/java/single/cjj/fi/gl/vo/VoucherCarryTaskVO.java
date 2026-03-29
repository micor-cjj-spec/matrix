package single.cjj.fi.gl.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherCarryTaskVO {
    private String code;
    private String name;
    private String status;
    private String message;
    private String actionHint;
}
