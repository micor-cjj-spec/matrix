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

## Question curation lifecycle

Benchmark questions support a two-stage lifecycle:

1. `INACTIVE`: a draft question that may be imported before its ground-truth document or chunk is known
2. `ACTIVE`: a labelled question that is eligible for benchmark execution

A question cannot become `ACTIVE` until it has at least one expected document ID or expected chunk ID. This prevents placeholder identifiers from contaminating Recall@K and MRR.

Bulk import:

- accepts at most 200 questions per request
- defaults unlabelled questions to `INACTIVE`
- defaults labelled questions to `ACTIVE`
- skips duplicate question text within the same dataset
- reports imported, skipped, and rejected rows independently

The starter file `docs/ai/evaluation/finance-baseline-v1.json` contains 50 finance-domain draft questions across general ledger, period close, AR/AP, expense, funds, master data, and reliability topics.

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
POST /api/ai/admin/knowledge/evaluations/datasets/{datasetId}/questions/bulk
PUT  /api/ai/admin/knowledge/evaluations/datasets/{datasetId}/questions/{questionId}

POST /api/ai/admin/knowledge/evaluations/datasets/{datasetId}/runs?topK=5
GET  /api/ai/admin/knowledge/evaluations/runs/{runId}
GET  /api/ai/admin/knowledge/evaluations/runs/{runId}/results
```

When knowledge ACL is enabled, evaluation management and execution use the existing global knowledge-operation guard. Only configured system administrators can manage or run a global benchmark.

## Example

Create a dataset:

```json
{
  "name": "财务月结核心问题集",
  "description": "验证月结、凭证、往来和报表知识召回",
  "status": "ACTIVE"
}
```

Bulk-import draft questions:

```json
{
  "questions": [
    {
      "question": "月结前必须完成哪些结账检查？",
      "kbIds": ["all"],
      "expectedDocIds": [],
      "expectedChunkIds": [],
      "expectedAnswer": "应检查未过账凭证、凭证断号、损益结转和往来核对。",
      "status": "INACTIVE"
    }
  ]
}
```

Label and activate a question:

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
4. Import the 50-question finance starter set as drafts.
5. Bind expected documents or chunks and activate at least ten representative questions.
6. Execute a Top-5 run and inspect both aggregate metrics and every zero-hit result.
7. Record the run ID before changing chunking, embeddings, retrieval weights, or vector-store configuration.
8. Re-run the same dataset and compare metrics and latency.

The migration only adds new tables and does not change existing knowledge retrieval behavior.

## Current boundary

Deferred to later evaluation phases:

- asynchronous and scheduled benchmark execution
- run-to-run comparison API and dashboard
- precision, NDCG, and category-level metrics
- generated-answer evaluation and citation faithfulness
- LLM-as-judge with deterministic rubrics
- benchmark export and dataset versioning
- reranker and retrieval-configuration experiment metadata
- release quality gates based on minimum metric thresholds
