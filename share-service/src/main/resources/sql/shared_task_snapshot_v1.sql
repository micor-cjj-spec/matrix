CREATE TABLE IF NOT EXISTS matrix_shared_task_snapshot (
    task_id VARCHAR(80) NOT NULL COMMENT '共享任务编号',
    payload_json LONGTEXT NOT NULL COMMENT '完整任务聚合 JSON',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    created_at DATETIME(3) NOT NULL COMMENT '任务创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '快照更新时间',
    PRIMARY KEY (task_id),
    KEY idx_shared_task_deleted_updated (deleted, updated_at),
    CONSTRAINT chk_shared_task_payload_json CHECK (JSON_VALID(payload_json))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='共享运营任务聚合快照';
