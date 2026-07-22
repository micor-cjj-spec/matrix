-- Matrix workflow phase 4 batch 1: dynamic assignees and unified task center
-- MySQL 8.0+

CREATE TABLE IF NOT EXISTS wf_task_candidate (
    id VARCHAR(40) PRIMARY KEY,
    task_id VARCHAR(40) NOT NULL,
    candidate_type VARCHAR(32) NOT NULL,
    candidate_value VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wf_task_candidate (task_id, candidate_type, candidate_value),
    KEY idx_wf_candidate_lookup (candidate_type, candidate_value, task_id),
    KEY idx_wf_candidate_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流共享任务候选用户与角色';

-- Backfill direct assignments created before this migration.
INSERT IGNORE INTO wf_task_candidate
    (id, task_id, candidate_type, candidate_value, created_at)
SELECT REPLACE(UUID(), '-', ''), id, assignee_type, assignee_value, created_at
FROM wf_task
WHERE assignee_type IN ('USER', 'ROLE')
  AND assignee_value IS NOT NULL
  AND assignee_value <> '';
