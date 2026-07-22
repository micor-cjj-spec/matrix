package single.cjj.scheduler.client.core;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

public class ExecutionRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExecutionRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void initializeSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS matrix_scheduler_execution_record (
                    fid BIGINT NOT NULL AUTO_INCREMENT,
                    fexecution_no VARCHAR(64) NOT NULL,
                    fhandler_code VARCHAR(128) NOT NULL,
                    fstatus VARCHAR(32) NOT NULL,
                    fstart_time DATETIME NULL,
                    fend_time DATETIME NULL,
                    fresult TEXT NULL,
                    ferror_message VARCHAR(2000) NULL,
                    fcreate_time DATETIME NOT NULL,
                    fupdate_time DATETIME NOT NULL,
                    PRIMARY KEY (fid),
                    UNIQUE KEY uk_scheduler_client_execution_no (fexecution_no)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    public StartResult tryStart(String executionNo, String handlerCode) {
        LocalDateTime now = LocalDateTime.now();
        try {
            jdbcTemplate.update("""
                    INSERT INTO matrix_scheduler_execution_record
                    (fexecution_no, fhandler_code, fstatus, fstart_time, fcreate_time, fupdate_time)
                    VALUES (?, ?, 'RUNNING', ?, ?, ?)
                    """, executionNo, handlerCode, now, now, now);
            return new StartResult(true, "RUNNING");
        } catch (DuplicateKeyException duplicate) {
            String status = jdbcTemplate.queryForObject(
                    "SELECT fstatus FROM matrix_scheduler_execution_record WHERE fexecution_no = ?",
                    String.class,
                    executionNo);
            return new StartResult(false, status);
        }
    }

    public void markSuccess(String executionNo, String resultJson) {
        jdbcTemplate.update("""
                UPDATE matrix_scheduler_execution_record
                   SET fstatus = 'SUCCESS', fend_time = ?, fresult = ?, ferror_message = NULL, fupdate_time = ?
                 WHERE fexecution_no = ? AND fstatus = 'RUNNING'
                """, LocalDateTime.now(), resultJson, LocalDateTime.now(), executionNo);
    }

    public void markFailed(String executionNo, String errorMessage) {
        jdbcTemplate.update("""
                UPDATE matrix_scheduler_execution_record
                   SET fstatus = 'FAILED', fend_time = ?, ferror_message = ?, fupdate_time = ?
                 WHERE fexecution_no = ? AND fstatus = 'RUNNING'
                """, LocalDateTime.now(), trim(errorMessage), LocalDateTime.now(), executionNo);
    }

    private String trim(String message) {
        if (message == null || message.length() <= 2000) {
            return message;
        }
        return message.substring(0, 2000);
    }

    public record StartResult(boolean started, String existingStatus) { }
}
