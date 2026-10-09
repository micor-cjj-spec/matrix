package single.cjj.bizfi.platform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_platform.matrix_platform_app")
public class MatrixPlatformApp {

    @TableId("fid")
    private Long fid;

    @TableField("fapp_code")
    private String fappCode;

    @TableField("fname")
    private String fname;

    @TableField("fdescription")
    private String fdescription;

    @TableField("fmeta")
    private String fmeta;

    @TableField("fstatus")
    private String fstatus;

    @TableField("fstatus_text")
    private String fstatusText;

    @TableField("froute_path")
    private String froutePath;

    @TableField("ficon_key")
    private String ficonKey;

    @TableField("faccent")
    private String faccent;

    @TableField("ffeatured")
    private Integer ffeatured;

    @TableField("favailable")
    private Integer favailable;

    @TableField("fnew_page")
    private Integer fnewPage;

    @TableField("fsort_no")
    private Integer fsortNo;

    @TableField("fcreate_time")
    private LocalDateTime fcreateTime;

    @TableField("fmodify_time")
    private LocalDateTime fmodifyTime;
}
