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
- `ORGANIZATION`: Matrix team ID (`bizfi_base_user.ftid`). It is issued in JWT `organizationIds` and mapped to `org:<id>`, `organization:<id>`, and `team:<id>` authorities.
- `AUTHORITY`: arbitrary Spring Security authority, for example `ROLE_FINANCE_KNOWLEDGE`, `team:88`, or `department:9`.

Team and department IDs remain distinct. Department IDs are never interpreted as organization IDs.

## JWT identity lifecycle

`auth-service` continues to issue claim `id` as the user principal and now includes:

- `organizationIds`: the user's `ftid`, when present
- `departmentIds`: the user's `fdptid`, when present

These dedicated claims are intentionally separate from `roles` and `authorities`, so Gateway does not forward teams or departments as `X-User-Roles`.

`base-service` also accepts externally issued identity claims:

- `authorities`
- `permissions`
- `roles`
- `role`
- `organizationIds`
- `organizationId`
- `orgIds`
- `orgId`
- `departmentIds`
- `departmentId`

Mapping rules:

- organization IDs become `org:<id>`, `organization:<id>`, and `team:<id>`
- department IDs become `department:<id>`
- role claims receive a `ROLE_` alias when the prefix is absent
- explicit authorities and permissions are preserved

Tokens without the new claims remain valid and can use USER ACL entries. Existing sessions must log in again before organization or department grants take effect. Tokens expire after one hour, so stale organization membership is bounded by the existing token lifetime.

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

1. Deploy Phase 3C `auth-service` and `base-service` with `AI_KNOWLEDGE_ACL_ENABLED=false`.
2. Verify existing login, knowledge CRUD, retrieval, ingestion, and indexing behavior.
3. Apply `sql/bizfi_ai_knowledge_acl_v6.sql`.
4. Configure at least one system administrator through `AI_KNOWLEDGE_ACL_ADMIN_USER_IDS` or `AI_KNOWLEDGE_ACL_ADMIN_AUTHORITIES`.
5. Insert at least one OWNER for every existing knowledge base. The V6 migration contains examples.
6. Add required organization, user, and authority grants.
7. Verify ACL entries through the API or frontend shield launcher.
8. Set `AI_KNOWLEDGE_ACL_ENABLED=true` and restart `base-service`.
9. Sign out and sign in again so the token includes current team and department claims.
10. Test one VIEWER, EDITOR, ADMIN, OWNER, and unauthorized account.
11. Deploy the Phase 3C frontend.

Re-run the existing-base bootstrap immediately before enabling ACL if knowledge bases were created while the feature flag remained disabled.

New knowledge bases created after ACL is enabled automatically grant the creator USER/OWNER in the same transaction.

## Safe disabled behavior

While ACL is disabled:

- knowledge listing and retrieval do not query the ACL table
- document CRUD and ingestion preserve the previous behavior
- new knowledge-base creation does not access the ACL table
- knowledge-base deletion does not access the ACL table
- the frontend shows rollout guidance instead of loading ACL members

ACL management endpoints themselves require the V6 table because they are used for bootstrap and administration.

## Query behavior

Interactive access resolution queries only ACL rows matching the current user ID, organization IDs, or authorities. The V6 indexes support these subject lookups. Invalid stored permission values fail closed and do not grant VIEWER access.

RAG scope resolution calculates:

```text
requested knowledge scope ∩ current principal's accessible knowledge bases
```

Legacy direct document-ID scopes are resolved back to their owning knowledge base before retrieval, preventing cross-base bypass.

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
- a team ORGANIZATION grant works only after the user obtains a refreshed token
- `department:<id>` can be used as an AUTHORITY grant without becoming an organization ID
- Gateway does not expose team or department claims as `X-User-Roles`
- non-admin users cannot list or change ACL members
- ADMIN cannot remove or demote the final OWNER
- global reindex is rejected for non-system administrators
- background indexing continues because worker execution is not tied to an interactive principal
