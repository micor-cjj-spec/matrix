package single.cjj.erp.procurement.reverse.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; import org.apache.ibatis.annotations.*;
import single.cjj.erp.procurement.reverse.entity.SupplierClaimEntity;
@Mapper
public interface SupplierClaimMapper extends BaseMapper<SupplierClaimEntity> {
 @Select("""
   SELECT * FROM matrix_erp_supplier_claim
    WHERE fid=#{fid} AND ftenant_id=#{tenantId} AND fdelete_flag=0
    FOR UPDATE
 """)
 SupplierClaimEntity selectByIdForUpdate(@Param("fid") Long fid,@Param("tenantId") String tenantId);
}
