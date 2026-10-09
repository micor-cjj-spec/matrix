package single.cjj.erp.procurement.inbound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import single.cjj.erp.procurement.inbound.entity.PurchaseInboundEntryEntity;

import java.util.List;

@Mapper
public interface PurchaseInboundEntryMapper extends BaseMapper<PurchaseInboundEntryEntity> {

    @Select("""
            SELECT *
              FROM matrix_erp_purchase_inbound_entry
             WHERE fid = #{fid}
               AND ftenant_id = #{tenantId}
               AND fdelete_flag = 0
             FOR UPDATE
            """)
    PurchaseInboundEntryEntity selectByIdForUpdate(
            @Param("fid") Long fid,
            @Param("tenantId") String tenantId
    );

    @Select("""
            SELECT e.*
              FROM matrix_erp_purchase_inbound_entry e
              JOIN matrix_erp_purchase_inbound h
                ON h.fid = e.fpurchase_inbound_id
               AND h.ftenant_id = e.ftenant_id
               AND h.fdelete_flag = 0
             WHERE e.ftenant_id = #{tenantId}
               AND e.fpurchase_order_entry_id = #{purchaseOrderEntryId}
               AND e.fdelete_flag = 0
               AND h.fstatus = 'CONFIRMED'
               AND h.fapproval_status = 'AUDITED'
             ORDER BY h.fdate ASC, h.fid ASC, e.fline_no ASC, e.fid ASC
            """)
    List<PurchaseInboundEntryEntity> selectConfirmedByPurchaseOrderEntry(
            @Param("tenantId") String tenantId,
            @Param("purchaseOrderEntryId") Long purchaseOrderEntryId
    );
}
