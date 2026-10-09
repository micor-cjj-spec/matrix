package single.cjj.erp.sales.commercial.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import single.cjj.erp.sales.commercial.workflow.SalesWorkflowLinkEntity;
import java.util.List;

@Mapper
public interface SalesWorkflowLinkMapper extends BaseMapper<SalesWorkflowLinkEntity> {
    @Select("""
            SELECT * FROM matrix_erp_sales_workflow_link
             WHERE fstatus IN ('PENDING', 'FAILED')
               AND (fnext_retry_time IS NULL OR fnext_retry_time <= NOW())
             ORDER BY fcreate_time LIMIT #{limit}
            """)
    List<SalesWorkflowLinkEntity> ready(@Param("limit") int limit);

    @Update("""
            UPDATE matrix_erp_sales_workflow_link SET fstatus='FAILED',
              fmodify_time=NOW(), fnext_retry_time=NOW()
             WHERE fstatus='STARTING'
               AND fmodify_time < DATE_SUB(NOW(), INTERVAL 10 MINUTE)
            """)
    int recoverStale();

    @Update("""
            UPDATE matrix_erp_sales_workflow_link SET fstatus='STARTING', fmodify_time=NOW()
             WHERE fid=#{id} AND fstatus IN ('PENDING', 'FAILED')
               AND (fnext_retry_time IS NULL OR fnext_retry_time <= NOW())
            """)
    int claim(@Param("id") Long id);

    @Update("""
            UPDATE matrix_erp_sales_workflow_link
               SET fstatus='ACTIVE', finstance_id=#{instanceId}, fmodify_time=NOW(),
                   fnext_retry_time=NULL
             WHERE fid=#{id} AND fstatus='STARTING'
               AND (finstance_id IS NULL OR finstance_id=#{instanceId})
            """)
    int activated(@Param("id") Long id, @Param("instanceId") String instanceId);

    @Update("""
            UPDATE matrix_erp_sales_workflow_link
               SET fstatus='FAILED', fretry_count=fretry_count+1,
                   fnext_retry_time=#{nextRetry}, fmodify_time=NOW()
             WHERE fid=#{id} AND fstatus='STARTING'
            """)
    int failed(@Param("id") Long id,
               @Param("nextRetry") java.time.LocalDateTime nextRetry);

    @Select("""
            SELECT * FROM matrix_erp_sales_workflow_link
             WHERE finstance_id=#{instanceId} FOR UPDATE
            """)
    SalesWorkflowLinkEntity lockByInstance(@Param("instanceId") String instanceId);

    @Update("""
            UPDATE matrix_erp_sales_workflow_link
               SET fstatus=#{status}, fprocessed_event_id=#{eventId},
                   fmodify_time=NOW()
             WHERE fid=#{id} AND fstatus='ACTIVE'
            """)
    int finish(@Param("id") Long id, @Param("status") String status,
               @Param("eventId") String eventId);

    @Select("""
            SELECT * FROM matrix_erp_sales_workflow_link
             WHERE ftenant_id=#{tenantId} AND fdocument_type=#{documentType}
               AND fdocument_id=#{documentId} LIMIT 1
            """)
    SalesWorkflowLinkEntity findDocument(@Param("tenantId") String tenantId,
            @Param("documentType") String documentType, @Param("documentId") Long documentId);
}
