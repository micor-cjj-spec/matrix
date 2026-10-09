package single.cjj.erp.sales.commercial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.sales.commercial.entity.SalesQuoteEntity;

@Mapper
public interface SalesQuoteMapper extends BaseMapper<SalesQuoteEntity> {
    @Select("""
            SELECT * FROM matrix_erp_sales_quote
             WHERE fid = #{fid} AND ftenant_id = #{tenantId} AND fdelete_flag = 0
             FOR UPDATE
            """)
    SalesQuoteEntity selectByIdForUpdate(@Param("fid") Long fid, @Param("tenantId") String tenantId);
}

