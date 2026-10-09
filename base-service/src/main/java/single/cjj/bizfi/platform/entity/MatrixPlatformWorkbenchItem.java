package single.cjj.bizfi.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_platform.matrix_platform_workbench_item")
public class MatrixPlatformWorkbenchItem {

    @TableId("fid")
    private Long fid;

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

    @TableField("ftag")
    private String ftag;

    @TableField("fitem_type")
    private String fitemType;

    @TableField("fpriority")
    private String fpriority;

    @TableField("froute_path")
    private String froutePath;

    @TableField("ficon_key")
    private String ficonKey;

    @TableField("faccent")
    private String faccent;

    @TableField("fstatus")
    private String fstatus;

    @TableField("fstatus_text")
    private String fstatusText;

    @TableField("favailable")
    private Integer favailable;

    @TableField("fnew_page")
    private Integer fnewPage;

    @TableField("ffeatured")
    private Integer ffeatured;

    @TableField("fsort_no")
    private Integer fsortNo;

    @TableField("fmeta_json")
    private String fmetaJson;

    @TableField("fcreate_time")
    private LocalDateTime fcreateTime;

    @TableField("fmodify_time")
    private LocalDateTime fmodifyTime;
}
