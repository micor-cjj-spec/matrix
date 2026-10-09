package single.cjj.erp.procurement.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.delivery.entity.SupplierDeliveryResponseEntity;

@Mapper
public interface SupplierDeliveryResponseMapper extends BaseMapper<SupplierDeliveryResponseEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_supplier_delivery_response
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    SupplierDeliveryResponseEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
