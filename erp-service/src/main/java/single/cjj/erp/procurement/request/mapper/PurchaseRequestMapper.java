package single.cjj.erp.procurement.request.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntity;

@Mapper
public interface PurchaseRequestMapper extends BaseMapper<PurchaseRequestEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_request
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseRequestEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
