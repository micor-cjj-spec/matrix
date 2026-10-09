package single.cjj.erp.sales.commercial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.sales.commercial.entity.SalesQuoteEntryEntity;

@Mapper
public interface SalesQuoteEntryMapper extends BaseMapper<SalesQuoteEntryEntity> {
    @Delete("""
            DELETE FROM matrix_erp_sales_quote_entry
             WHERE fquote_id = #{quoteId} AND ftenant_id = #{tenantId}
            """)
    int deleteDraftEntries(@Param("quoteId") Long quoteId, @Param("tenantId") String tenantId);
}
