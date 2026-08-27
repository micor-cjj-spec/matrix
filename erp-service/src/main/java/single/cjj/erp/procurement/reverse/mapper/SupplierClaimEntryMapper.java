package single.cjj.erp.procurement.reverse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.reverse.entity.SupplierClaimEntryEntity;

@Mapper
public interface SupplierClaimEntryMapper extends BaseMapper<SupplierClaimEntryEntity> {
    @Select("""
            SELECT *
              FROM matrix_erp_supplier_claim_entry
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    SupplierClaimEntryEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
