package single.cjj.erp.procurement.acceptance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.acceptance.entity.PurchaseAcceptanceEntryEntity;

@Mapper
public interface PurchaseAcceptanceEntryMapper extends BaseMapper<PurchaseAcceptanceEntryEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_acceptance_entry
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseAcceptanceEntryEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
