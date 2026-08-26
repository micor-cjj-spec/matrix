package single.cjj.erp.procurement.receipt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.receipt.entity.PurchaseReceiptEntryEntity;

@Mapper
public interface PurchaseReceiptEntryMapper extends BaseMapper<PurchaseReceiptEntryEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_receipt_entry
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseReceiptEntryEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
