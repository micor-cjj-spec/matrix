package single.cjj.erp.sales.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.sales.contract.entity.SalesContractEntity;

@Mapper
public interface SalesContractMapper extends BaseMapper<SalesContractEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_sales_contract
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    SalesContractEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
