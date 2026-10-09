# P0-IMP-02 API 摘要

ERP：

```text
/api/procurement/purchase-receipts
/api/procurement/purchase-acceptances
/api/procurement/purchase-inbounds
```

三类单据均提供：创建、修改、详情、分页、submit、confirm、reject、cancel。

BOTP 内部 ERP 接口：

```text
GET  /api/procurement/internal/botp/documents/{documentType}/{fid}
GET  /api/procurement/internal/botp/targets/{documentType}/by-idempotency
POST /api/procurement/internal/botp/targets/{documentType}
```

BOTP 关系查询增强：

```text
GET /api/botp/relations/{relationId}
GET /api/botp/relations/{relationId}/entries
```
