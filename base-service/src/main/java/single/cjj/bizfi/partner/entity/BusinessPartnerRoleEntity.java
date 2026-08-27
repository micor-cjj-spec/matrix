package single.cjj.bizfi.partner.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("matrix_base_business_partner_role")
public class BusinessPartnerRoleEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long fbusinessPartnerId;
    private String froleType;
    private String fstatus;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
