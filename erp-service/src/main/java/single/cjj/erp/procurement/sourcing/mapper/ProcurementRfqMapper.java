package single.cjj.erp.procurement.sourcing.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; import org.apache.ibatis.annotations.*;
import single.cjj.erp.procurement.sourcing.entity.ProcurementRfqEntity;
@Mapper
public interface ProcurementRfqMapper extends BaseMapper<ProcurementRfqEntity> {
 @Select("""SELECT * FROM matrix_erp_procurement_rfq WHERE fid=#{fid} AND ftenant_id=#{tenantId} AND fdelete_flag=0 FOR UPDATE""")
 ProcurementRfqEntity selectByIdForUpdate(@Param("fid") Long fid,@Param("tenantId") String tenantId);
}
