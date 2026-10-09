# Matrix Knowledge Phase 3B: File Ingestion and Async Indexing

## Goal

Phase 3B adds controlled file ingestion and persistent asynchronous vector indexing on top of the Phase 3A knowledge-base model.

Supported formats in the first release:

- PDF with an extractable text layer
- Microsoft Word DOC and DOCX
- TXT
- Markdown (`.md`, `.markdown`)

The original file bytes are not stored in MySQL. The service stores the extracted text in the existing knowledge document table and stores file metadata plus indexing state in `bizfi_ai_knowledge_index_job`.

## Deployment order

1. Deploy Phase 3A and apply `sql/bizfi_ai_knowledge_base_v4.sql`.
2. Back up the AI knowledge tables.
3. Apply `sql/bizfi_ai_knowledge_ingestion_v5.sql`.
4. Deploy the Phase 3B backend with ingestion still disabled.
5. Verify the application starts and existing knowledge retrieval remains healthy.
6. Set `AI_KNOWLEDGE_INGESTION_ENABLED=true` and restart `base-service`.
7. Deploy the Phase 3B frontend.
8. Import one TXT or Markdown document and verify the job reaches `SUCCEEDED` or `SKIPPED` when semantic retrieval is intentionally disabled.
9. Import representative PDF and DOCX samples.

The feature is disabled by default. While disabled, the worker, job list, retry endpoint and manual enqueue path do not access the V5 table.

## Configuration

```text
AI_KNOWLEDGE_INGESTION_ENABLED=true
AI_KNOWLEDGE_MAX_FILE_SIZE=10MB
AI_KNOWLEDGE_MAX_REQUEST_SIZE=11MB
AI_KNOWLEDGE_MAX_FILE_SIZE_BYTES=10485760
AI_KNOWLEDGE_MAX_EXTRACTED_CHARACTERS=2000000
AI_KNOWLEDGE_INDEX_MAX_ATTEMPTS=3
AI_KNOWLEDGE_INDEX_BATCH_SIZE=5
AI_KNOWLEDGE_INDEX_POLL_DELAY_MS=5000
AI_KNOWLEDGE_INDEX_STALE_MINUTES=15
```

The multipart limit and application byte limit should remain aligned. The request limit is slightly larger to allow multipart metadata.

## API

### Capability

```http
GET /api/ai/knowledge/ingestion/config
```

### Import

```http
POST /api/ai/knowledge/import
Content-Type: multipart/form-data
```

Parts and parameters:

- `file`: required
- `kbId`: defaults to `default`
- `title`: optional; file name is used when omitted
- `category`: optional
- `version`: optional
- `status`: defaults to `ACTIVE`

Document creation, chunk creation and indexing-job creation share one MySQL transaction.

### Jobs

```http
GET  /api/ai/knowledge/index-jobs
POST /api/ai/knowledge/index-jobs/{jobId}/retry
POST /api/ai/knowledge/docs/{docId}/index-jobs
```

All paths inherit the Phase 3A authenticated `/ai/knowledge/**` security rule.

## Job states

- `PENDING`: available for claiming after `nextRetryTime`
- `RUNNING`: atomically claimed by one application instance
- `SUCCEEDED`: vector indexing completed
- `PARTIAL`: MySQL metadata completed but PGVector write was partial
- `SKIPPED`: semantic retrieval is disabled; keyword chunks remain available
- `FAILED`: retry limit reached or manually unresolved

The worker uses conditional updates for multi-instance claiming. Failed attempts use bounded exponential backoff. Stale running jobs are requeued while attempts remain and are marked failed after the maximum attempt count.

## File safety boundary

The parser enforces:

- extension allowlist
- file size limit before parsing
- content-type and extension consistency
- extracted text character limit
- normalized safe file name
- rejection of empty extracted text
- SHA-256 content hash recording

Scanned image-only PDFs are rejected because OCR is not part of Phase 3B.

## Rollback

1. Set `AI_KNOWLEDGE_INGESTION_ENABLED=false`.
2. Restart `base-service`.
3. Roll back the frontend if its import panel should be hidden.

Existing imported documents remain ordinary knowledge documents and continue to support keyword or vector retrieval. The V5 job table may remain in place; removing it is not required for rollback.

## Current boundary

Phase 3B does not yet include:

- OCR for scanned PDFs or images
- object-storage retention of original files
- antivirus scanning
- tenant and organization ACL propagation
- bulk ZIP ingestion
- semantic chunking and reranking
- RAG evaluation dashboards
