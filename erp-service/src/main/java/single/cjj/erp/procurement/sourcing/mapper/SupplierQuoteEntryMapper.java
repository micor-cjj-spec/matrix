package single.cjj.erp.procurement.sourcing.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; import org.apache.ibatis.annotations.*;
import single.cjj.erp.procurement.sourcing.entity.SupplierQuoteEntryEntity;
@Mapper
public interface SupplierQuoteEntryMapper extends BaseMapper<SupplierQuoteEntryEntity> {
 @Select("""SELECT * FROM matrix_erp_supplier_quote_entry WHERE fid=#{fid} AND ftenant_id=#{tenantId} AND fdelete_flag=0 FOR UPDATE""")
 SupplierQuoteEntryEntity selectByIdForUpdate(@Param("fid") Long fid,@Param("tenantId") String tenantId);
}
