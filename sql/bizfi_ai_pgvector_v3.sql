-- Matrix RAG Phase 2 PostgreSQL migration
-- Target database: matrix_ai
-- Default embedding model: text-embedding-3-small (1536 dimensions)

CREATE EXTENSION IF NOT EXISTS vector;

CREATE SCHEMA IF NOT EXISTS knowledge;

CREATE TABLE IF NOT EXISTS knowledge.matrix_ai_knowledge_vector (
    fid BIGSERIAL PRIMARY KEY,
    fchunk_id VARCHAR(160) NOT NULL,
    fdocument_id VARCHAR(128) NOT NULL,
    ftenant_id BIGINT,
    forganization_id BIGINT,
    fcategory VARCHAR(64),
    fpermission_scope VARCHAR(64),
    fcontent TEXT NOT NULL,
    fcontent_hash VARCHAR(64) NOT NULL,
    fmetadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    fembedding VECTOR(1536) NOT NULL,
    fembedding_model VARCHAR(128) NOT NULL,
    fembedding_dimensions INT NOT NULL,
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    fcreate_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fmodify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_matrix_ai_vector_chunk_model UNIQUE (fchunk_id, fembedding_model),
    CONSTRAINT ck_matrix_ai_vector_dimensions CHECK (fembedding_dimensions = 1536)
);

CREATE INDEX IF NOT EXISTS idx_matrix_ai_vector_hnsw
    ON knowledge.matrix_ai_knowledge_vector
    USING hnsw (fembedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS idx_matrix_ai_vector_document
    ON knowledge.matrix_ai_knowledge_vector (fdocument_id);

CREATE INDEX IF NOT EXISTS idx_matrix_ai_vector_scope
    ON knowledge.matrix_ai_knowledge_vector (
        ftenant_id,
        forganization_id,
        fstatus
    );

CREATE INDEX IF NOT EXISTS idx_matrix_ai_vector_metadata
    ON knowledge.matrix_ai_knowledge_vector
    USING gin (fmetadata);
