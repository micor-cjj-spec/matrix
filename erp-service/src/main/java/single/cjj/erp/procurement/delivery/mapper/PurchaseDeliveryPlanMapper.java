package single.cjj.erp.procurement.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.delivery.entity.PurchaseDeliveryPlanEntity;

@Mapper
public interface PurchaseDeliveryPlanMapper extends BaseMapper<PurchaseDeliveryPlanEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_delivery_plan
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseDeliveryPlanEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );

    @Select("""
            SELECT p.*
              FROM matrix_erp_purchase_delivery_plan p
              JOIN matrix_erp_purchase_delivery_plan_entry e
                ON e.fdelivery_plan_id = p.fid
               AND e.fdelete_flag = 0
             WHERE e.ftenant_id = #{tenantId}
               AND e.fpurchase_order_entry_id = #{purchaseOrderEntryId}
               AND p.fstatus <> 'CANCELLED'
               AND p.fdelete_flag = 0
             ORDER BY p.fid DESC
             LIMIT 1
             FOR UPDATE
            """)
    PurchaseDeliveryPlanEntity selectActiveByOrderEntryForUpdate(
            @Param("tenantId") String tenantId,
            @Param("purchaseOrderEntryId") Long purchaseOrderEntryId
    );
}
