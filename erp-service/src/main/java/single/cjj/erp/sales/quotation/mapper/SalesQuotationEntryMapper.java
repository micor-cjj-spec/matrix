package single.cjj.erp.sales.quotation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.sales.quotation.entity.SalesQuotationEntryEntity;

@Mapper
public interface SalesQuotationEntryMapper extends BaseMapper<SalesQuotationEntryEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_sales_quotation_entry
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    SalesQuotationEntryEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );
}
