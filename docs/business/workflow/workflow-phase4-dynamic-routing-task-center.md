# 工作流第四期第一批：动态审批人、组合条件与统一任务中心

## 1. 动态审批人

人工节点的 `assigneeRule.type` 支持：

- `USER`：固定用户，`value` 为用户 ID。
- `ROLE`：固定角色，`value` 为角色编码。
- `VARIABLE`：流程变量中的单个或多个用户。
- `INITIATOR`：流程发起人。
- `USERS`：静态用户候选列表，使用 `values`。
- `ROLES`：静态角色候选列表，使用 `values`。
- `USERS_VARIABLE`：流程变量中的用户候选列表。
- `ROLES_VARIABLE`：流程变量中的角色候选列表。

示例：

```json
{
  "key": "financeReview",
  "name": "财务审核",
  "type": "USER_TASK",
  "assigneeRule": {
    "type": "USERS_VARIABLE",
    "value": "approval.financeReviewers"
  }
}
```

解析出的用户和角色写入 `wf_task_candidate`。如果只有一个候选人，任务继续保留直接 `USER/ROLE` 指派；如果存在多个候选人，则任务使用 `CANDIDATE/MULTIPLE`，所有候选人共享同一任务。第一个合法候选人完成任务后，数据库乐观锁阻止其他候选人重复处理。

## 2. 角色授权

网关会删除客户端传入的以下身份头，并根据 JWT 重新生成：

- `X-User-Id`
- `X-Tenant-Id`
- `X-User-Roles`

角色声明按顺序读取：`roles`、`roleCodes`、`authorities`、`role`。角色值可以是数组、集合或逗号分隔字符串。

工作流服务处理角色任务时，同时校验：

1. 请求中的 `operatorId` 与可信 `X-User-Id` 一致。
2. 用户或角色存在于 `wf_task_candidate`。
3. 任务仍为 `PENDING/CLAIMED`。
4. 任务版本未被其他请求更新。

## 3. 组合条件

连线条件支持嵌套 `ALL/ANY`，并支持点路径读取流程变量。

```json
{
  "logic": "ALL",
  "children": [
    {
      "field": "amount",
      "operator": "GE",
      "value": 5000
    },
    {
      "logic": "ANY",
      "children": [
        {
          "field": "applicant.region",
          "operator": "EQ",
          "value": "WEST"
        },
        {
          "field": "urgent",
          "operator": "EQ",
          "value": true
        }
      ]
    }
  ]
}
```

仍支持无条件默认连线。多条条件连线按照 `priority` 从高到低匹配。

## 4. 统一任务中心

```http
GET /api/workflow/task-center
```

请求头：

```http
X-User-Id: user-1001
X-User-Roles: FINANCE_REVIEWER,FINANCE_MANAGER
```

查询参数：

- `tenantId`
- `view=TODO|DONE|INITIATED`
- `businessType`，可选
- `keyword`，可选，匹配业务 ID、流程定义 Key、任务名称
- `page`，默认 1
- `size`，默认 20，最大 100

视图定义：

- `TODO`：直接分配给当前用户，或当前用户角色命中的共享候选任务。
- `DONE`：当前用户实际执行过审批动作的任务。
- `INITIATED`：当前用户发起的流程，并返回每个实例最近的任务信息。

## 5. 数据库迁移

在既有工作流 SQL 后执行：

```text
workflow-service/src/main/resources/sql/workflow_v3_dynamic_routing.sql
```

迁移会创建 `wf_task_candidate`，并把历史 `USER/ROLE` 直接任务回填为候选记录。

## 6. 当前边界

这一批实现的是共享候选任务，不是会签。多个候选人中任意一人处理后任务结束。全部通过、比例通过和并行分支将在后续会签/并行网关版本中实现。
