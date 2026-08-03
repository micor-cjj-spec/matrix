# RAG Evaluation Phase 3D

## Goal

Phase 3D creates a repeatable retrieval-quality baseline for Matrix knowledge bases. It evaluates the existing production retrieval chain rather than a separate test implementation:

```text
keyword retrieval
+ semantic retrieval (MySQL JSON or PGVector)
+ weighted Reciprocal Rank Fusion
+ ACL knowledge scope
```

The evaluation feature is disabled by default and does not access V7 tables until explicitly enabled.

## Data model

Apply:

```text
sql/bizfi_ai_rag_evaluation_v7.sql
```

The migration creates:

- `bizfi_ai_rag_eval_set`: named evaluation sets bound to one knowledge base
- `bizfi_ai_rag_eval_case`: standard questions and expected documents/chunks
- `bizfi_ai_rag_eval_run`: persistent asynchronous run state and aggregate metrics
- `bizfi_ai_rag_eval_result`: immutable per-case result snapshots

A case must contain at least one expected document ID or expected chunk ID. When chunk expectations are present, chunk matching takes precedence over document matching.

## Metrics

### Hit@K

A case is a hit when at least one expected item appears in the first K retrieved citations.

```text
Hit@K = hit cases / completed cases
```

### Mean Reciprocal Rank (MRR)

The reciprocal rank for one case is:

```text
1 / rank of first relevant result
```

A miss contributes zero. MRR is the mean across all completed cases.

### Recall@K

```text
matched unique expected items / total unique expected items
```

This is calculated per case and averaged across the run.

### Latency

Each case records end-to-end retrieval latency. Runs expose average latency and nearest-rank P95 latency.

Cases that fail because of provider, embedding, database, or parsing errors are persisted with the error message and counted as misses. A run with any case errors is `PARTIAL`.

## Permissions

Evaluation management requires knowledge-base `ADMIN` or `OWNER` permission. System administrators retain the configured ACL bypass.

The scheduled worker runs the retrieval call under a controlled `ROLE_SUPER_ADMIN` security context. This is required because scheduled threads have no interactive JWT principal. The worker still restricts every request to the run's single knowledge base ID.

## APIs

```text
GET    /api/ai/knowledge/evaluation/config
GET    /api/ai/knowledge/evaluation/sets?kbId={kbId}
POST   /api/ai/knowledge/evaluation/sets?kbId={kbId}
PUT    /api/ai/knowledge/evaluation/sets/{setId}
DELETE /api/ai/knowledge/evaluation/sets/{setId}

GET    /api/ai/knowledge/evaluation/sets/{setId}/cases
POST   /api/ai/knowledge/evaluation/sets/{setId}/cases
PUT    /api/ai/knowledge/evaluation/sets/{setId}/cases/{caseId}
DELETE /api/ai/knowledge/evaluation/sets/{setId}/cases/{caseId}

GET    /api/ai/knowledge/evaluation/sets/{setId}/runs
POST   /api/ai/knowledge/evaluation/sets/{setId}/runs
GET    /api/ai/knowledge/evaluation/runs/{runId}
GET    /api/ai/knowledge/evaluation/runs/{runId}/results
POST   /api/ai/knowledge/evaluation/runs/{runId}/retry
```

## Run lifecycle

```text
PENDING -> RUNNING -> SUCCEEDED
                   -> PARTIAL
                   -> FAILED
```

- only one `PENDING` or `RUNNING` run is allowed per evaluation set
- evaluation cases cannot be changed while a run is active
- runs are claimed with a conditional status update for multi-instance safety
- stale `RUNNING` runs are returned to `PENDING`
- retry clears previous results and reuses the same run ID
- each run snapshots semantic flags, RRF weights, vector backend, and fallback settings

## Configuration

```yaml
bizfi:
  ai:
    knowledge-evaluation:
      enabled: ${AI_KNOWLEDGE_EVALUATION_ENABLED:false}
      poll-delay-ms: ${AI_KNOWLEDGE_EVALUATION_POLL_DELAY_MS:10000}
      batch-size: ${AI_KNOWLEDGE_EVALUATION_BATCH_SIZE:1}
      max-cases-per-set: ${AI_KNOWLEDGE_EVALUATION_MAX_CASES:100}
      stale-running-minutes: ${AI_KNOWLEDGE_EVALUATION_STALE_MINUTES:30}
```

## Safe rollout

1. Deploy the backend with `AI_KNOWLEDGE_EVALUATION_ENABLED=false`.
2. Confirm existing knowledge CRUD, ingestion, ACL, and retrieval remain healthy.
3. Apply `sql/bizfi_ai_rag_evaluation_v7.sql`.
4. Set `AI_KNOWLEDGE_EVALUATION_ENABLED=true` and restart `base-service`.
5. Deploy the Phase 3D frontend.
6. Create one evaluation set for the default or finance knowledge base.
7. Start with 20–50 real questions from finance operations and interviews.
8. Run the baseline and record Hit@K, MRR, Recall@K, average latency, and P95.
9. Change only one retrieval variable at a time, then rerun the same set.

## Recommended initial acceptance gates

These are starting guardrails rather than universal targets:

```text
Hit@5 >= 0.80
MRR >= 0.60
Recall@5 >= 0.75
P95 retrieval latency <= 1500 ms
case execution errors = 0
```

Targets should be calibrated using Matrix's own finance corpus and question distribution.

## Rollback

Set:

```text
AI_KNOWLEDGE_EVALUATION_ENABLED=false
```

and restart `base-service`. Do not drop V7 tables during an incident; evaluation data is isolated from the production retrieval path.
