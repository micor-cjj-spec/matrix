package single.cjj.erp.procurement.inbound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntryEntity;

@Mapper
public interface PurchaseInboundEntryMapper extends BaseMapper<PurchaseInboundEntryEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_inbound_entry
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseInboundEntryEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
