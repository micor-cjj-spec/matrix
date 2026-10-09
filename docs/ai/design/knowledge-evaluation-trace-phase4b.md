# Matrix Knowledge Evaluation Trace Phase 4B

## Goal

Phase 4B makes every new benchmark explainable. A traceable run preserves the keyword candidates, semantic candidates, RRF contributions, final fused order, vector backend routing, fallback reason, embedding model, and a deterministic retrieval-configuration fingerprint.

The trace is diagnostic evidence. It does not change the citations returned by the existing hybrid retriever.

## Migration

Apply in order:

1. `sql/bizfi_ai_knowledge_evaluation_v7.sql`
2. `sql/bizfi_ai_knowledge_evaluation_trace_v8.sql`

The V8 table is append-only at the application layer and stores one trace per run/question result.

## Traceable run API

```http
POST /api/ai/admin/knowledge/evaluations/datasets/{datasetId}/trace-runs?topK=5
GET  /api/ai/admin/knowledge/evaluations/runs/{runId}/traces
```

Traceable runs still write the normal evaluation run and result tables, so the existing summary and result endpoints remain valid:

```http
GET /api/ai/admin/knowledge/evaluations/runs/{runId}
GET /api/ai/admin/knowledge/evaluations/runs/{runId}/results
```

## Retrieval modes

- `HYBRID_RRF`: keyword and semantic candidates were fused.
- `KEYWORD_ONLY`: semantic retrieval was disabled, not applicable, or returned no candidates without an execution failure.
- `KEYWORD_FALLBACK`: semantic retrieval failed and fail-open returned keyword candidates.
- `UNAVAILABLE`: the retriever could not provide a trace or the question failed before a trace was produced.

PGVector-to-MySQL fallback remains `HYBRID_RRF` when the MySQL JSON semantic fallback succeeds. The trace records configured backend `pgvector`, actual backend `mysql-json`, `fallbackUsed=true`, and the original failure reason.

## Configuration fingerprint

The 16-character SHA-256 prefix includes:

- requested Top K and candidate Top K
- semantic retrieval enabled/fail-open flags
- semantic candidate limit and minimum score
- keyword and semantic weights
- RRF K
- configured vector store
- vector read-fallback flag

This fingerprint allows benchmark runs to be grouped by effective retrieval configuration without exposing credentials or connection strings.

## Candidate evidence

Each keyword and semantic candidate records:

- source and source rank
- document and chunk identifiers
- snippet
- source-specific RRF contribution
- total fused score
- whether it entered final Top K

The fused candidate list records the complete deterministic RRF order, including candidates outside final Top K.

## Deployment

The development helper now applies V7 and V8:

```bash
MYSQL_CONTAINER=matrix-mysql \
MYSQL_DATABASE=matrix_open_api \
MYSQL_PWD=... \
bash scripts/apply-dev-db-scripts.sh
```

After deployment, execute a new traceable benchmark. Historical V7 runs do not have trace rows and are intentionally not backfilled because the original candidate lists and fallback path cannot be reconstructed reliably.
