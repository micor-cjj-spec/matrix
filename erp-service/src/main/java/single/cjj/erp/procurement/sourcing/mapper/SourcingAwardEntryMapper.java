package single.cjj.erp.procurement.sourcing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.sourcing.entity.SourcingAwardEntryEntity;

@Mapper
public interface SourcingAwardEntryMapper extends BaseMapper<SourcingAwardEntryEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_sourcing_award_entry
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    SourcingAwardEntryEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
