package single.cjj.erp.procurement.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.invoice.entity.SupplierInvoiceEntity;

@Mapper
public interface SupplierInvoiceMapper extends BaseMapper<SupplierInvoiceEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_supplier_invoice
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    SupplierInvoiceEntity selectByIdForUpdate(@Param("fid") Long fid, @Param("tenantId") String tenantId);
}
