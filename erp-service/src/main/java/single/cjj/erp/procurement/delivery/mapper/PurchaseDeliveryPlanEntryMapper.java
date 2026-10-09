package single.cjj.erp.procurement.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.delivery.entity.PurchaseDeliveryPlanEntryEntity;

import java.util.List;

@Mapper
public interface PurchaseDeliveryPlanEntryMapper extends BaseMapper<PurchaseDeliveryPlanEntryEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_delivery_plan_entry
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseDeliveryPlanEntryEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_delivery_plan_entry
             WHERE fdelivery_plan_id = #{planId}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             ORDER BY fline_no
             FOR UPDATE
            """)
    List<PurchaseDeliveryPlanEntryEntity> selectByPlanIdForUpdate(
            @Param("planId") Long planId,
            @Param("tenantId") String tenantId
    );

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_delivery_plan_entry
             WHERE fdelivery_plan_id = #{planId}
               AND fpurchase_order_entry_id = #{purchaseOrderEntryId}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             ORDER BY COALESCE(fcommitted_delivery_date, fplanned_delivery_date), fline_no
             FOR UPDATE
            """)
    List<PurchaseDeliveryPlanEntryEntity> selectByOrderEntryForUpdate(
            @Param("planId") Long planId,
            @Param("tenantId") String tenantId,
            @Param("purchaseOrderEntryId") Long purchaseOrderEntryId
    );
}
