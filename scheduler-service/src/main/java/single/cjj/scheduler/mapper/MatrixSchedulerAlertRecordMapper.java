package single.cjj.scheduler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import single.cjj.scheduler.entity.MatrixSchedulerAlertRecord;

public interface MatrixSchedulerAlertRecordMapper extends BaseMapper<MatrixSchedulerAlertRecord> {

    @Insert("""
            INSERT IGNORE INTO matrix_scheduler_alert_record (
                fid,fdedupe_key,fexecution_no,fjob_id,fexecutor_code,falert_type,flevel,
                ftitle,fcontent,fstatus,fack_by,fack_time,fcreate_time,fupdate_time
            ) VALUES (
                #{fid},#{fdedupeKey},#{fexecutionNo},#{fjobId},#{fexecutorCode},#{falertType},#{flevel},
                #{ftitle},#{fcontent},#{fstatus},#{fackBy},#{fackTime},#{fcreateTime},#{fupdateTime}
            )
            """)
    int insertIgnore(MatrixSchedulerAlertRecord alert);
}
