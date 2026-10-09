package single.cjj.erp.procurement.reverse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.reverse.entity.PurchaseDeductionEntity;

@Mapper
public interface PurchaseDeductionMapper extends BaseMapper<PurchaseDeductionEntity> {
    @Select("""
            SELECT *
              FROM matrix_erp_purchase_deduction
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseDeductionEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
