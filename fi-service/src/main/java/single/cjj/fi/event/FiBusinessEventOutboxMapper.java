package single.cjj.fi.event;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FiBusinessEventOutboxMapper extends BaseMapper<FiBusinessEventOutboxEntity> {

    @Select("""
            SELECT *
              FROM matrix_fi_business_event_outbox
             WHERE fdelete_flag = 0
               AND fstatus IN ('PENDING','FAILED')
               AND (fnext_retry_time IS NULL OR fnext_retry_time <= #{now})
             ORDER BY fid
             LIMIT #{limit}
            """)
    List<FiBusinessEventOutboxEntity> findDue(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    @Update("""
            UPDATE matrix_fi_business_event_outbox
               SET fstatus = 'PUBLISHING',
                   fclaim_token = #{claimToken},
                   fclaim_time = #{now},
                   fmodify_time = #{now},
                   fversion = fversion + 1
             WHERE fid = #{fid}
               AND fdelete_flag = 0
               AND fstatus IN ('PENDING','FAILED')
               AND (fnext_retry_time IS NULL OR fnext_retry_time <= #{now})
            """)
    int claim(
            @Param("fid") Long fid,
            @Param("claimToken") String claimToken,
            @Param("now") LocalDateTime now
    );

    @Update("""
            UPDATE matrix_fi_business_event_outbox
               SET fstatus = 'PUBLISHED',
                   fsent_time = #{now},
                   fclaim_token = NULL,
                   fclaim_time = NULL,
                   flast_error = NULL,
                   fmodify_time = #{now},
                   fversion = fversion + 1
             WHERE fid = #{fid}
               AND fstatus = 'PUBLISHING'
               AND fclaim_token = #{claimToken}
            """)
    int markPublished(
            @Param("fid") Long fid,
            @Param("claimToken") String claimToken,
            @Param("now") LocalDateTime now
    );

    @Update("""
            UPDATE matrix_fi_business_event_outbox
               SET fstatus = #{status},
                   fretry_count = #{retryCount},
                   fnext_retry_time = #{nextRetryTime},
                   fclaim_token = NULL,
                   fclaim_time = NULL,
                   flast_error = #{error},
                   fmodify_time = #{now},
                   fversion = fversion + 1
             WHERE fid = #{fid}
               AND fstatus = 'PUBLISHING'
               AND fclaim_token = #{claimToken}
            """)
    int markFailure(
            @Param("fid") Long fid,
            @Param("claimToken") String claimToken,
            @Param("status") String status,
            @Param("retryCount") int retryCount,
            @Param("nextRetryTime") LocalDateTime nextRetryTime,
            @Param("error") String error,
            @Param("now") LocalDateTime now
    );

    @Update("""
            UPDATE matrix_fi_business_event_outbox
               SET fstatus = 'FAILED',
                   fclaim_token = NULL,
                   fclaim_time = NULL,
                   fnext_retry_time = #{now},
                   flast_error = 'STALE_PUBLISH_CLAIM_RECOVERED',
                   fmodify_time = #{now},
                   fversion = fversion + 1
             WHERE fstatus = 'PUBLISHING'
               AND fclaim_time < #{deadline}
            """)
    int recoverStaleClaims(
            @Param("deadline") LocalDateTime deadline,
            @Param("now") LocalDateTime now
    );
}
