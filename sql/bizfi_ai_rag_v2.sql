-- Matrix AI Knowledge RAG V2
-- Phase 1 persists provider-neutral embedding vectors as JSON in MySQL.
-- Apply after sql/bizfi_ai_v1.sql and before enabling AI_SEMANTIC_RETRIEVAL_ENABLED.

ALTER TABLE bizfi_ai_knowledge_chunk
    ADD COLUMN fembedding LONGTEXT NULL COMMENT 'JSON encoded embedding vector' AFTER fkeywords,
    ADD COLUMN fembeddingmodel VARCHAR(128) NULL COMMENT 'Embedding model used to build the vector' AFTER fembedding,
    ADD COLUMN fembeddingdimensions INT NULL COMMENT 'Embedding vector dimensions' AFTER fembeddingmodel,
    ADD COLUMN fembeddingtime DATETIME NULL COMMENT 'Latest successful embedding time' AFTER fembeddingdimensions;

CREATE INDEX idx_knowledge_chunk_embedding_model
    ON bizfi_ai_knowledge_chunk (fembeddingmodel, fembeddingdimensions);
