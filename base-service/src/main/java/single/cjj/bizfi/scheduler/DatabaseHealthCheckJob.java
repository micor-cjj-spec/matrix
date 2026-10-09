package single.cjj.bizfi.scheduler;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import single.cjj.scheduler.client.annotation.MatrixJobHandler;
import single.cjj.scheduler.client.core.JobContext;
import single.cjj.scheduler.client.core.JobResult;
import single.cjj.scheduler.client.core.MatrixJob;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@MatrixJobHandler(value = "database-health-check", name = "数据库健康检查")
public class DatabaseHealthCheckJob implements MatrixJob {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthCheckJob(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public JobResult execute(JobContext context) {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        if (result == null || result != 1) {
            return JobResult.failure("DATABASE_UNHEALTHY", "数据库健康检查未返回预期结果");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("executionNo", context.getExecutionNo());
        data.put("checkedAt", LocalDateTime.now().toString());
        data.put("database", "UP");
        return JobResult.success(data);
    }
}
