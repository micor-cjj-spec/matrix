package single.cjj.erp.procurement.sourcing.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; import org.apache.ibatis.annotations.*;
import single.cjj.erp.procurement.sourcing.entity.ProcurementRfqEntryEntity;
@Mapper
public interface ProcurementRfqEntryMapper extends BaseMapper<ProcurementRfqEntryEntity> {
 @Select("""SELECT * FROM matrix_erp_procurement_rfq_entry WHERE fid=#{fid} AND ftenant_id=#{tenantId} AND fdelete_flag=0 FOR UPDATE""")
 ProcurementRfqEntryEntity selectByIdForUpdate(@Param("fid") Long fid,@Param("tenantId") String tenantId);
}
