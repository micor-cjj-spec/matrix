package single.cjj.workflow.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import single.cjj.workflow.engine.WorkflowAssigneeResolver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class WorkflowTaskCandidateRepository {

    private final JdbcTemplate jdbcTemplate;

    public WorkflowTaskCandidateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertCandidates(String taskId,
                                 List<WorkflowAssigneeResolver.Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (WorkflowAssigneeResolver.Candidate candidate : candidates) {
            jdbcTemplate.update("""
                    INSERT IGNORE INTO wf_task_candidate
                        (id, task_id, candidate_type, candidate_value)
                    VALUES (?, ?, ?, ?)
                    """, newId(), taskId, candidate.type(), candidate.value());
        }
    }

    public List<CandidateRow> findByTaskId(String taskId) {
        return jdbcTemplate.query("""
                SELECT task_id, candidate_type, candidate_value
                FROM wf_task_candidate
                WHERE task_id = ?
                ORDER BY candidate_type, candidate_value
                """, this::mapCandidate, taskId);
    }

    public boolean canOperate(String taskId, String userId, Set<String> roleCodes) {
        Set<String> roles = roleCodes == null ? Set.of() : new LinkedHashSet<>(roleCodes);
        return findByTaskId(taskId).stream().anyMatch(candidate ->
                ("USER".equals(candidate.candidateType())
                        && candidate.candidateValue().equals(userId))
                        || ("ROLE".equals(candidate.candidateType())
                        && roles.contains(candidate.candidateValue()))
        );
    }

    private CandidateRow mapCandidate(ResultSet rs, int rowNum) throws SQLException {
        return new CandidateRow(
                rs.getString("task_id"),
                rs.getString("candidate_type"),
                rs.getString("candidate_value")
        );
    }

    private String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public record CandidateRow(String taskId,
                               String candidateType,
                               String candidateValue) {
    }
}
