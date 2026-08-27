package single.cjj.erp.crm.lead.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.crm.lead.entity.CrmLeadEntity;

@Mapper
public interface CrmLeadMapper extends BaseMapper<CrmLeadEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_crm_lead
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    CrmLeadEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
