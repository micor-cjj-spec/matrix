package single.cjj.erp.crm.opportunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.crm.opportunity.entity.CrmOpportunityEntity;

@Mapper
public interface CrmOpportunityMapper extends BaseMapper<CrmOpportunityEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_crm_opportunity
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    CrmOpportunityEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
