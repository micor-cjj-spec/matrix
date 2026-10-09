package single.cjj.erp.procurement.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.order.entity.PurchaseOrderEntryEntity;

@Mapper
public interface PurchaseOrderEntryMapper extends BaseMapper<PurchaseOrderEntryEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_order_entry
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseOrderEntryEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
