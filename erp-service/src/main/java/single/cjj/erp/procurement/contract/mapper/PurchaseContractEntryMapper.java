package single.cjj.erp.procurement.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntryEntity;

@Mapper
public interface PurchaseContractEntryMapper extends BaseMapper<PurchaseContractEntryEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_contract_entry
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseContractEntryEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
