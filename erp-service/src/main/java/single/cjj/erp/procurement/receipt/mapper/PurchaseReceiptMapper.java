package single.cjj.erp.procurement.receipt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.receipt.entity.PurchaseReceiptEntity;

@Mapper
public interface PurchaseReceiptMapper extends BaseMapper<PurchaseReceiptEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_receipt
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseReceiptEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
