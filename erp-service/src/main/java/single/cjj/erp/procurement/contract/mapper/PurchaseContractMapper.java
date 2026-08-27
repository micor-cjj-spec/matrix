package single.cjj.erp.procurement.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.contract.entity.PurchaseContractEntity;

@Mapper
public interface PurchaseContractMapper extends BaseMapper<PurchaseContractEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_contract
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseContractEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
