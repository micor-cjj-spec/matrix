# Matrix Knowledge Evaluation Phase 4A

## Goal

Phase 4A introduces a persistent retrieval evaluation foundation for the Matrix knowledge platform. It turns retrieval quality from a subjective manual check into a repeatable benchmark with historical runs and per-question evidence.

The first release evaluates retrieval only. Answer generation, LLM-as-judge scoring, faithfulness, and reranker experiments remain follow-up work.

## Data model

Migration: `sql/bizfi_ai_knowledge_evaluation_v7.sql`

Tables:

- `bizfi_ai_evaluation_dataset`: named benchmark set and lifecycle status
- `bizfi_ai_evaluation_question`: standard question, retrieval scope, expected documents/chunks, and optional reference answer
- `bizfi_ai_evaluation_run`: one immutable benchmark execution summary
- `bizfi_ai_evaluation_result`: full retrieved citations and metrics for one question in one run

Expected chunk IDs take precedence over expected document IDs when both are provided. This allows coarse document-level benchmarks to coexist with precise chunk-level benchmarks.

## Metrics

Each question records:

- Recall@K: unique expected documents or chunks retrieved within Top-K divided by the expected set size
- Reciprocal rank: `1 / first relevant rank`, or zero when no relevant result is returned
- First relevant rank
- Retrieval latency in milliseconds
- Full citation payload for diagnosis
- Error message when retrieval fails

Each run aggregates:

- mean Recall@K
- mean reciprocal rank (MRR)
- zero-hit rate
- average retrieval latency
- completed and total question counts

A run is:

- `SUCCEEDED` when every question completes
- `PARTIAL` when some questions fail
- `FAILED` when every question fails

A single failed question never aborts the remaining benchmark.

## API

All endpoints are authenticated and live under the knowledge administration namespace:

```http
GET  /api/ai/admin/knowledge/evaluations/datasets
POST /api/ai/admin/knowledge/evaluations/datasets

GET  /api/ai/admin/knowledge/evaluations/datasets/{datasetId}/questions
POST /api/ai/admin/knowledge/evaluations/datasets/{datasetId}/questions

POST /api/ai/admin/knowledge/evaluations/datasets/{datasetId}/runs?topK=5
GET  /api/ai/admin/knowledge/evaluations/runs/{runId}
GET  /api/ai/admin/knowledge/evaluations/runs/{runId}/results
```

When knowledge ACL is enabled, evaluation management and execution use the existing global knowledge-operation guard. Only configured system administrators can run a global benchmark.

## Example

Create a dataset:

```json
{
  "name": "财务月结核心问题集",
  "description": "验证月结、凭证、往来和报表知识召回",
  "status": "ACTIVE"
}
```

Add a chunk-level question:

```json
{
  "question": "月结前必须完成哪些结账检查？",
  "kbIds": ["finance-policy"],
  "expectedDocIds": ["month-end-close-policy"],
  "expectedChunkIds": ["month-end-close-policy_3"],
  "expectedAnswer": "应检查未过账凭证、凭证断号、损益结转和往来核对。",
  "status": "ACTIVE"
}
```

Run the benchmark:

```http
POST /api/ai/admin/knowledge/evaluations/datasets/{datasetId}/runs?topK=5
```

## Deployment

1. Back up the Matrix AI tables.
2. Apply `sql/bizfi_ai_knowledge_evaluation_v7.sql`.
3. Deploy `base-service`.
4. Create a small benchmark dataset with at least ten representative questions.
5. Execute a Top-5 run and inspect both the aggregate metrics and every zero-hit result.
6. Record the run ID before changing chunking, embeddings, retrieval weights, or vector-store configuration.
7. Re-run the same dataset and compare metrics and latency.

The migration only adds new tables and does not change existing knowledge retrieval behavior.

## Current boundary

Deferred to later evaluation phases:

- asynchronous and scheduled benchmark execution
- run-to-run comparison API and dashboard
- precision, NDCG, and category-level metrics
- generated-answer evaluation and citation faithfulness
- LLM-as-judge with deterministic rubrics
- benchmark import/export
- reranker and retrieval-configuration experiment metadata
- release quality gates based on minimum metric thresholds
