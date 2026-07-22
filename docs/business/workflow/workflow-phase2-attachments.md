# Workflow Phase 2 — Attachment Center

## Goal

Provide a workflow-owned attachment metadata and relation layer without coupling workflow APIs to a specific object-storage vendor.

The development provider stores bytes on the local filesystem. Upload and preview URLs are HMAC-signed and time limited. A MinIO, OSS, COS, or S3 provider can later implement `WorkflowStorageProvider` without changing the external API.

## Upload lifecycle

```text
PENDING -> STORED -> UPLOADED
```

1. The business client requests an upload URL.
2. Workflow Service creates `wf_file` and `wf_attachment_relation` records.
3. The client performs a direct `PUT` to the signed URL.
4. Workflow Service streams the body to the configured storage provider and verifies size/SHA-256.
5. The client confirms the upload.
6. Only `UPLOADED` and `CLEAN` files satisfy workflow attachment checks.

## Database migration

Run:

```text
workflow-service/src/main/resources/sql/workflow_v2_attachments.sql
```

It creates:

- `wf_file`
- `wf_attachment_relation`

## Request an upload URL

```http
POST /api/workflow/files/upload-url
Content-Type: application/json
```

```json
{
  "tenantId": "default",
  "sourceSystem": "fi-service",
  "businessType": "expense_claim",
  "businessId": "EXP202607220028",
  "categoryCode": "INVOICE",
  "fileName": "invoice.pdf",
  "contentType": "application/pdf",
  "fileSize": 42861,
  "operatorId": "10001"
}
```

The response contains a time-limited `PUT` URL.

## Upload bytes

```http
PUT {uploadUrl}
Content-Type: application/pdf
Content-Length: 42861

<binary body>
```

## Confirm upload

```http
POST /api/workflow/files/{fileId}/confirm
Content-Type: application/json
```

```json
{
  "operatorId": "10001",
  "sha256": "optional-client-sha256"
}
```

Confirmation is idempotent. The response contains a time-limited preview/download URL.

## Query attachments

By business object:

```http
GET /api/workflow/files/business/fi-service/expense_claim/EXP202607220028?tenantId=default
```

By process instance:

```http
GET /api/workflow/files/instances/{instanceId}
```

Attachments uploaded before process startup are visible by instance because the repository matches the workflow instance's tenant/source/business tuple.

## Delete an attachment relation

```http
DELETE /api/workflow/files/relations/{relationId}?operatorId=10001
```

Deletion is logical. The relation remains available for audit but no longer satisfies required-category checks.

## Required-category node configuration

Backward-compatible single-file rules:

```json
{
  "requiredCategories": ["INVOICE", "PAYMENT_PROOF"]
}
```

Quantity-aware rules:

```json
{
  "requiredCategories": [
    {"category": "INVOICE", "minimumCount": 2},
    {"category": "PAYMENT_PROOF", "minimumCount": 1}
  ]
}
```

The `attachment-check` handler reads persisted, confirmed, clean attachment relations. Legacy `attachmentCategories` variables are still accepted during migration.

## Configuration

```yaml
workflow:
  attachment:
    base-url: http://localhost:10006
    local-root: ./data/workflow-attachments
    signing-secret: replace-in-every-environment
    upload-ttl-seconds: 900
    download-ttl-seconds: 300
    max-file-size-bytes: 52428800
```

Production must replace `WORKFLOW_ATTACHMENT_SIGNING_SECRET` and use an HTTPS external base URL.

## Deferred

- MinIO/OSS provider implementation and bucket lifecycle policies
- malware scanning integration; the local provider marks confirmed files `CLEAN`
- administrator deletion and retention rules
- multipart/chunked uploads for very large files
- image OCR and invoice recognition
