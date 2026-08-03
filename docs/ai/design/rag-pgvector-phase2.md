# Matrix RAG PGVector Phase 2

## Goal

Move semantic retrieval from bounded MySQL JSON vector scans to PostgreSQL + PGVector HNSW search without changing the public chat or knowledge retrieval APIs.

This phase is intentionally deployed through dual write and feature flags. The MySQL JSON implementation remains available as a temporary semantic fallback until PGVector data and query quality are verified.

## Components

- `ai-service`: provider-neutral embedding generation, unchanged from Phase 1.
- `base-service`: chunk indexing, dual-write orchestration, PGVector retrieval, RRF fusion and fallback.
- MySQL: knowledge document/chunk business records and temporary JSON vectors.
- PostgreSQL + PGVector: indexed semantic vectors.

## Local database

```bash
cd deploy/pgvector
PGVECTOR_PASSWORD=change_me docker compose up -d
```

The compose stack mounts `sql/bizfi_ai_pgvector_v3.sql` and initializes:

- `vector` extension
- `knowledge` schema
- `knowledge.matrix_ai_knowledge_vector`
- HNSW cosine index
- document, scope and JSONB metadata indexes

The committed schema uses `VECTOR(1536)` for `text-embedding-3-small`. A different embedding dimension requires a matching migration and `AI_EMBEDDING_DIMENSIONS` value before indexing.

## Feature flags

```text
AI_SEMANTIC_RETRIEVAL_ENABLED=true
AI_PGVECTOR_ENABLED=true
AI_VECTOR_STORE_TYPE=mysql-json
AI_VECTOR_DUAL_WRITE_ENABLED=true
AI_VECTOR_READ_FALLBACK_ENABLED=true
AI_PGVECTOR_JDBC_URL=jdbc:postgresql://127.0.0.1:5433/matrix_ai
AI_PGVECTOR_USERNAME=matrix_ai
AI_PGVECTOR_PASSWORD=change_me
AI_EMBEDDING_DIMENSIONS=1536
```

## Rollout

### 1. Start PGVector and apply the schema

Verify the table and HNSW index exist before starting `base-service` with PGVector enabled.

### 2. Enable dual write while reads remain on MySQL JSON

```text
AI_VECTOR_STORE_TYPE=mysql-json
AI_VECTOR_DUAL_WRITE_ENABLED=true
```

New document creation, updates and chunk rebuilds write both stores. Existing user queries still use the Phase 1 MySQL JSON semantic implementation.

### 3. Backfill existing active knowledge

Authenticated endpoint:

```http
POST /api/ai/admin/knowledge/vector-store/migrations/pgvector
```

Configuration/status endpoint:

```http
GET /api/ai/admin/knowledge/vector-store/status
```

The migration regenerates embeddings and replaces all PGVector rows per document transactionally. It returns total documents, indexed documents, indexed chunks and failed document IDs.

### 4. Switch semantic reads to PGVector

```text
AI_VECTOR_STORE_TYPE=pgvector
AI_VECTOR_DUAL_WRITE_ENABLED=true
AI_VECTOR_READ_FALLBACK_ENABLED=true
```

Keyword retrieval and weighted RRF stay unchanged. Only the semantic candidate source changes from application-side cosine scans to PGVector HNSW search.

### 5. Disable the legacy path after observation

After verifying query latency, error rate, Top-K quality and document counts:

```text
AI_VECTOR_STORE_TYPE=pgvector
AI_VECTOR_DUAL_WRITE_ENABLED=false
AI_VECTOR_READ_FALLBACK_ENABLED=false
```

The MySQL embedding columns should be removed only in a later migration after the fallback period ends.

## Rollback

No data migration rollback is required. Change:

```text
AI_VECTOR_STORE_TYPE=mysql-json
AI_VECTOR_DUAL_WRITE_ENABLED=false
```

Restart `base-service`. Existing keyword and MySQL JSON semantic retrieval remain available.

## Failure behavior

- PGVector write failure with `AI_SEMANTIC_FAIL_OPEN=true`: MySQL indexing succeeds and the index result is `PARTIAL`.
- PGVector read failure with `AI_VECTOR_READ_FALLBACK_ENABLED=true`: the request uses MySQL JSON semantic retrieval.
- Semantic failure with `AI_SEMANTIC_FAIL_OPEN=true`: the request still returns keyword retrieval results.
- Embedding dimension mismatch: indexing fails before writing PGVector.

## Current boundary

Phase 2 does not yet include:

- asynchronous indexing jobs
- PDF/Word ingestion
- tenant and organization values populated from the request context
- reranking
- RAG evaluation datasets and quality dashboards
- removal of MySQL JSON vectors
