package single.cjj.erp.procurement.request.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.request.entity.PurchaseRequestEntryEntity;

@Mapper
public interface PurchaseRequestEntryMapper extends BaseMapper<PurchaseRequestEntryEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_request_entry
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseRequestEntryEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
