# Matrix Knowledge Base Phase 3A

## Goal

Introduce a real knowledge-base aggregate above knowledge documents while keeping all existing chat and retrieval API paths compatible.

Phase 3A covers organization and retrieval scope. It does not yet add file ingestion, tenant permissions, or asynchronous indexing.

## Data model

New table:

- `bizfi_ai_knowledge_base`
  - stable `fkbid`
  - display name and description
  - `ACTIVE` / `INACTIVE` status
  - create and modify timestamps

Existing `bizfi_ai_knowledge_doc` receives `fkbid`.

Migration `sql/bizfi_ai_knowledge_base_v4.sql`:

1. creates the knowledge-base table
2. creates the built-in `default` knowledge base
3. adds `fkbid` to documents
4. assigns all existing documents to `default`
5. adds the knowledge-base/status document index

Apply after `sql/bizfi_ai_v1.sql` and before deploying the Phase 3A application build.

## Retrieval scope semantics

`kbIds` now accepts a mixture of:

- knowledge-base IDs, which expand to their active documents
- legacy document IDs, which remain supported
- `all`, `knowledge`, or `bizfi`, which mean unrestricted retrieval

`default` is no longer a global alias. It identifies the built-in default knowledge base. This is backward compatible at rollout because all existing documents are migrated into `default`.

Inactive knowledge bases resolve to no documents and never fall back to a same-named legacy document ID.

The same scope resolver is used by:

- keyword retrieval
- MySQL JSON semantic fallback
- PostgreSQL PGVector semantic retrieval

## APIs

Knowledge-base management:

```http
GET    /api/ai/knowledge/bases
POST   /api/ai/knowledge/bases
PUT    /api/ai/knowledge/bases/{kbId}
DELETE /api/ai/knowledge/bases/{kbId}
```

Document APIs remain under `/api/ai/knowledge/docs`. The request and response models add `kbId`, and list requests accept an optional `kbId` filter.

The built-in `default` knowledge base cannot be deleted. Any knowledge base that still owns documents cannot be deleted.

## Frontend behavior

The knowledge workbench presents:

- knowledge-base navigation and CRUD
- document filtering and assignment
- document/chunk detail
- chunk rebuild and vector reindex actions
- retrieval preview by all bases, one base, or one document
- deep links from AI citations to the exact highlighted chunk

The AI assistant allows one or more knowledge bases to be selected for a conversation request.

## Deployment order

1. Back up the MySQL AI knowledge tables.
2. Apply `sql/bizfi_ai_knowledge_base_v4.sql`.
3. Deploy the backend.
4. Verify `GET /api/ai/knowledge/bases` returns `default`.
5. Verify existing documents return `kbId=default`.
6. Deploy the frontend.
7. Test all-base, one-base, and one-document retrieval.
8. Test an AI citation deep link and chunk highlight.

PGVector data does not require a schema migration in Phase 3A. Retrieval expands knowledge-base IDs to document IDs through MySQL before issuing the vector query.

## Rollback

The frontend can be rolled back independently because backend additions are compatible.

For backend rollback, keep the V4 table and document column in place. Older application builds ignore the extra `fkbid` column. Do not drop the column until all Phase 3A-created documents have been reassigned or exported.

## Current boundary

Still deferred to later Phase 3 work:

- PDF, Word, Markdown, and TXT upload/parse pipelines
- asynchronous indexing jobs and retry state machine
- tenant, organization, role, and document visibility enforcement
- knowledge-base-specific embedding and retrieval configuration
- reranking and evaluation dashboards
