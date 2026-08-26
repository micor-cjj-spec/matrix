package single.cjj.botp.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("matrix_botp_document_relation_entry")
public class BotpDocumentRelationEntryEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long fid;
    private String ftenantId;
    private Long frelationId;
    private String fsourceEntryId;
    private String ftargetEntryId;
    private BigDecimal fquantity;
    private BigDecimal famount;
    private BigDecimal fbaseQuantity;
    private BigDecimal fbaseAmount;
    private String frelationStatus;
    private Long fcreateBy;
    private LocalDateTime fcreateTime;
    private Long fmodifyBy;
    private LocalDateTime fmodifyTime;
    @TableLogic
    private Integer fdeleteFlag;
    @Version
    private Integer fversion;
}
