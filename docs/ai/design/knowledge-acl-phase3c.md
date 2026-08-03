# Knowledge ACL Phase 3C

## Goal

Phase 3C isolates knowledge bases by user, organization, or authentication authority without changing the existing document, chunk, MySQL JSON vector, or PGVector storage model.

The ACL feature is disabled by default. Existing deployments continue to behave as before until the V6 table is created, existing bases are bootstrapped, and the feature flag is enabled.

## Permission hierarchy

Permissions are cumulative:

| Permission | Capabilities |
| --- | --- |
| `VIEWER` | List the knowledge base, read documents/chunks, retrieve citations, view indexing jobs |
| `EDITOR` | All VIEWER capabilities plus create/update/delete documents, import files, rebuild chunks and enqueue/retry document indexing |
| `ADMIN` | All EDITOR capabilities plus update knowledge-base metadata and manage non-owner ACL entries |
| `OWNER` | All ADMIN capabilities plus grant/revoke OWNER and delete the knowledge base |

The highest matching permission wins.

## ACL subjects

`bizfi_ai_knowledge_base_acl` supports:

- `USER`: numeric Matrix user ID. It is matched against the authenticated principal.
- `ORGANIZATION`: organization ID. It is matched against JWT authorities such as `org:88`, `org_88`, `organization:88`, or `organization_88`.
- `AUTHORITY`: arbitrary Spring Security authority, for example `ROLE_FINANCE_KNOWLEDGE`.

## JWT claim mapping

`JwtAuthenticationFilter` continues to use claim `id` as the principal and additionally reads:

- `authorities`
- `permissions`
- `roles`
- `role`
- `organizationIds`
- `organizationId`
- `orgIds`
- `orgId`

Organization claims are converted to both `org:<id>` and `organization:<id>` authorities. Role claims also receive a `ROLE_` alias when the prefix is absent.

Tokens without these claims remain valid and can use USER ACL entries.

## Protected paths

ACL checks cover:

- knowledge-base listing, update, and delete
- document list, detail, create, update, delete, chunks, and categories
- keyword retrieval
- MySQL JSON semantic retrieval
- PGVector semantic retrieval
- file import
- indexing-job list and retry
- manual document indexing
- vector rebuild operations

When ACL is enabled, global reindex operations are restricted to configured system administrators.

## API

```text
GET    /api/ai/knowledge/bases/{kbId}/access
GET    /api/ai/knowledge/bases/{kbId}/acl
PUT    /api/ai/knowledge/bases/{kbId}/acl
DELETE /api/ai/knowledge/bases/{kbId}/acl/{aclId}
```

Grant example:

```json
{
  "subjectType": "ORGANIZATION",
  "subjectId": "88",
  "permission": "EDITOR"
}
```

## Configuration

```yaml
bizfi:
  ai:
    knowledge-acl:
      enabled: ${AI_KNOWLEDGE_ACL_ENABLED:false}
      admin-user-ids: ${AI_KNOWLEDGE_ACL_ADMIN_USER_IDS:}
      admin-authorities: ${AI_KNOWLEDGE_ACL_ADMIN_AUTHORITIES:ROLE_ADMIN,ROLE_SUPER_ADMIN,admin,super_admin}
```

System-administrator configuration is a global bypass and should be limited to operational accounts.

## Deployment order

1. Deploy Phase 3C backend with `AI_KNOWLEDGE_ACL_ENABLED=false`.
2. Verify existing knowledge CRUD, retrieval, ingestion, and indexing behavior.
3. Apply `sql/bizfi_ai_knowledge_acl_v6.sql`.
4. Configure at least one system administrator through `AI_KNOWLEDGE_ACL_ADMIN_USER_IDS` or `AI_KNOWLEDGE_ACL_ADMIN_AUTHORITIES`.
5. Insert at least one OWNER for every existing knowledge base. The V6 migration contains examples.
6. Add required organization, user, and authority grants.
7. Verify ACL entries through the API or frontend shield launcher.
8. Set `AI_KNOWLEDGE_ACL_ENABLED=true` and restart `base-service`.
9. Test one VIEWER, EDITOR, ADMIN, OWNER, and unauthorized account.
10. Deploy the Phase 3C frontend.

New knowledge bases created after ACL is enabled automatically grant the creator USER/OWNER in the same transaction.

## Safe disabled behavior

While ACL is disabled:

- knowledge listing and retrieval do not query the ACL table
- document CRUD and ingestion preserve the previous behavior
- new knowledge-base creation does not access the ACL table
- knowledge-base deletion does not access the ACL table
- the frontend shows rollout guidance instead of loading ACL members

ACL management endpoints themselves require the V6 table because they are used for bootstrap and administration.

## Rollback

Set:

```text
AI_KNOWLEDGE_ACL_ENABLED=false
```

and restart `base-service`.

Do not drop the ACL table during an incident. ACL rows are independent metadata and can remain in place while access filtering is disabled.

## Operational checks

After enabling ACL, validate:

- unauthorized users receive no knowledge bases and no retrieval citations
- legacy document-ID retrieval cannot cross into an inaccessible knowledge base
- organization authorities grant the expected effective permission
- non-admin users cannot list or change ACL members
- ADMIN cannot remove or demote the final OWNER
- global reindex is rejected for non-system administrators
- background indexing continues because worker execution is not tied to an interactive principal
