package single.cjj.erp.procurement.reverse.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; import org.apache.ibatis.annotations.*;
import single.cjj.erp.procurement.reverse.entity.PurchaseReturnEntryEntity;
@Mapper
public interface PurchaseReturnEntryMapper extends BaseMapper<PurchaseReturnEntryEntity> {
 @Select("""
   SELECT * FROM matrix_erp_purchase_return_entry
    WHERE fid=#{fid} AND ftenant_id=#{tenantId} AND fdelete_flag=0
    FOR UPDATE
 """)
 PurchaseReturnEntryEntity selectByIdForUpdate(@Param("fid") Long fid,@Param("tenantId") String tenantId);
}
