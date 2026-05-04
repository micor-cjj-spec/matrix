package single.cjj.bizfi.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_platform.matrix_platform_menu")
public class MatrixPlatformMenu {

    @TableId("fid")
    private Long fid;

    @TableField("fparent_id")
    private Long fparentId;

    @TableField("fapp_code")
    private String fappCode;

    @TableField("fmodule_code")
    private String fmoduleCode;

    @TableField("fmenu_code")
    private String fmenuCode;

    @TableField("fname")
    private String fname;

    @TableField("fdescription")
    private String fdescription;

    @TableField("fsummary")
    private String fsummary;

    @TableField("feyebrow")
    private String feyebrow;

    @TableField("fmenu_type")
    private String fmenuType;

    @TableField("froute_path")
    private String froutePath;

    @TableField("ficon_key")
    private String ficonKey;

    @TableField("fstatus")
    private String fstatus;

    @TableField("fstatus_text")
    private String fstatusText;

    @TableField("favailable")
    private Integer favailable;

    @TableField("fsort_no")
    private Integer fsortNo;

    @TableField("fmeta_json")
    private String fmetaJson;

    @TableField("fcreate_time")
    private LocalDateTime fcreateTime;

    @TableField("fmodify_time")
    private LocalDateTime fmodifyTime;
}
