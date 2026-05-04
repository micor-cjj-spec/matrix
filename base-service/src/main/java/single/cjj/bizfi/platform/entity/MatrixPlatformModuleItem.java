package single.cjj.bizfi.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_platform.matrix_platform_module_item")
public class MatrixPlatformModuleItem {

    @TableId("fid")
    private Long fid;

    @TableField("fapp_code")
    private String fappCode;

    @TableField("fmodule_code")
    private String fmoduleCode;

    @TableField("fsection")
    private String fsection;

    @TableField("fname")
    private String fname;

    @TableField("fdescription")
    private String fdescription;

    @TableField("fvalue")
    private String fvalue;

    @TableField("fhint")
    private String fhint;

    @TableField("fstatus")
    private String fstatus;

    @TableField("fstatus_text")
    private String fstatusText;

    @TableField("froute_path")
    private String froutePath;

    @TableField("ficon_key")
    private String ficonKey;

    @TableField("fprimary_flag")
    private Integer fprimaryFlag;

    @TableField("fsort_no")
    private Integer fsortNo;

    @TableField("fmeta_json")
    private String fmetaJson;

    @TableField("fcreate_time")
    private LocalDateTime fcreateTime;

    @TableField("fmodify_time")
    private LocalDateTime fmodifyTime;
}
