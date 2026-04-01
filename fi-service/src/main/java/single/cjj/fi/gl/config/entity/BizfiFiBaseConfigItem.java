package single.cjj.fi.gl.config.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_fi_base_config_item")
public class BizfiFiBaseConfigItem implements Serializable {
    @TableId("fid")
    private Long fid;
    private Long forg;
    private String fcategory;
    private String fcode;
    private String fname;
    private String fvalue;
    private String fstatus;
    private Integer fsort;
    private String fremark;
    private LocalDateTime fcreatetime;
    private LocalDateTime fupdatetime;
}
