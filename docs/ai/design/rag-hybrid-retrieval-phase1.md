# Matrix RAG Hybrid Retrieval Phase 1

## Goal

Upgrade the existing keyword-only knowledge retrieval path into a provider-neutral hybrid RAG foundation without changing the public chat API.

Phase 1 intentionally keeps the current MySQL knowledge tables and stores embedding vectors as JSON. This allows semantic retrieval to be validated before introducing PostgreSQL and PGVector in Phase 2.

## Runtime flow

```text
Knowledge create/update/rebuild
  -> Markdown-aware chunking (existing)
  -> base-service batches chunk text
  -> POST ai-service /internal/model/embeddings
  -> Spring AI EmbeddingModel
  -> persist vector/model/dimensions/time on each chunk

User question
  -> existing keyword retrieval
  -> query embedding
  -> cosine similarity over indexed candidates
  -> weighted Reciprocal Rank Fusion
  -> Top-K citations
  -> existing chat orchestration and prompt augmentation
```

## New internal API

```text
POST /api/internal/model/embeddings
X-Matrix-Internal-Token: <AI_INTERNAL_TOKEN>
Content-Type: application/json
```

Request:

```json
{
  "texts": ["月结前应完成凭证过账", "检查损益结转状态"]
}
```

Response:

```json
{
  "model": "text-embedding-3-small",
  "dimensions": 1536,
  "vectors": [[0.01, 0.02], [0.03, 0.04]]
}
```

The endpoint accepts at most 32 texts per request and uses the same internal token boundary as the model chat endpoints.

## Database migration

Apply before deploying the new base-service artifact:

```text
sql/bizfi_ai_rag_v2.sql
```

The migration adds:

- `fembedding`
- `fembeddingmodel`
- `fembeddingdimensions`
- `fembeddingtime`

Do not deploy the entity change before applying the migration because MyBatis-Plus selects mapped entity columns even when semantic retrieval is disabled.

## Configuration

ai-service:

```text
AI_API_KEY=
AI_BASE_URL=https://api.openai.com
AI_EMBEDDING_MODEL=text-embedding-3-small
AI_INTERNAL_TOKEN=
```

base-service:

```text
AI_INTERNAL_TOKEN=
AI_SEMANTIC_RETRIEVAL_ENABLED=false
AI_SEMANTIC_FAIL_OPEN=true
AI_SEMANTIC_CANDIDATE_LIMIT=500
AI_SEMANTIC_MIN_SCORE=0.50
AI_HYBRID_KEYWORD_WEIGHT=1.0
AI_HYBRID_SEMANTIC_WEIGHT=1.0
AI_HYBRID_RRF_K=60
AI_EMBEDDING_BATCH_SIZE=16
```

Keep semantic retrieval disabled until the migration is applied and ai-service embedding calls are verified.

## Index operations

Reindex one document:

```text
POST /api/ai/knowledge/docs/{docId}/reindex-vector
```

Reindex existing documents with missing vectors:

```text
POST /api/ai/knowledge/reindex?onlyMissing=true
```

Creating, updating, or rebuilding a knowledge document automatically attempts vector indexing when semantic retrieval is enabled. With fail-open enabled, indexing failures leave keyword retrieval available.

## Rollout

1. Apply `sql/bizfi_ai_rag_v2.sql`.
2. Deploy ai-service with an embedding-capable OpenAI-compatible provider.
3. Verify the internal embedding endpoint with two short texts.
4. Deploy base-service with `AI_SEMANTIC_RETRIEVAL_ENABLED=false`.
5. Enable semantic retrieval in a controlled environment.
6. Run `POST /api/ai/knowledge/reindex?onlyMissing=true`.
7. Compare keyword-only and hybrid retrieval using a fixed evaluation question set.
8. Increase the candidate limit only after observing latency and memory.

## Safety and fallback

- The public chat API is unchanged.
- Existing keyword retrieval remains the fallback path.
- Stored vectors record their model and dimensions.
- Query vectors are not compared with chunks indexed by a different named model.
- Invalid or malformed vectors are skipped when fail-open is enabled.
- No prompt, user identity, conversation content, or finance transaction data is stored in vector metadata.

## Current limitations

- Semantic similarity is calculated inside base-service over a bounded MySQL candidate set.
- Vectors are stored as JSON and do not have an ANN index.
- Indexing is synchronous after document changes.
- No PDF, Word, OCR, or URL ingestion is included.
- No reranker or RAG evaluation dashboard is included.
- Knowledge document authorization remains a separate security phase.

## Phase 2

Replace JSON vector scanning with PostgreSQL and PGVector:

- Spring AI PGVector VectorStore
- metadata filters for tenant, organization, category, status, version, and document ID
- HNSW or exact search based on corpus size
- asynchronous indexing tasks
- stale vector cleanup and model-version reindexing
- retrieval metrics and evaluation sets
