# 单据下推反写字段说明

## 规则定义

| 字段 | 说明 |
|---|---|
| ruleCode | 规则稳定编码，全局唯一 |
| ruleName | 规则名称 |
| version | 规则版本号 |
| status | DRAFT、TESTING、PUBLISHED、DISABLED、ARCHIVED |
| sourceSystemCode | 源系统编码 |
| sourceDocumentType | 源单据类型编码 |
| targetSystemCode | 目标系统编码 |
| targetDocumentType | 目标单据类型编码 |
| headerMappings | 单头字段映射 |
| entryMappings | 分录字段映射 |
| writebackMappings | 反写映射 |

## 字段映射

| 字段 | 说明 |
|---|---|
| sourceType | SOURCE_FIELD、CONSTANT、CONTEXT |
| sourcePath | 源字段路径，如 header.supplierId |
| targetPath | 目标字段路径，如 header.payeeId |
| constantValue | 常量值 |
| required | 映射结果是否必填 |

## 执行请求

| 字段 | 说明 |
|---|---|
| requestId | 调用方请求号 |
| sourceSystem | 来源系统 |
| tenantId | 租户 ID |
| ruleCode | 转换规则编码 |
| sourceDocuments | 源单引用列表 |
| parameters | 执行参数和上下文 |
| executionMode | SYNC 或 ASYNC |
| callbackUrl | 异步回调地址 |

## 单据引用

| 字段 | 说明 |
|---|---|
| systemCode | 单据所属系统 |
| documentType | 单据类型 |
| documentId | 单据主键 |
| entryIds | 选中的分录主键 |

## 执行结果

| 字段 | 说明 |
|---|---|
| executionId | BOTP 执行 ID |
| status | CREATED、TRANSFORMING、TARGET_CREATING、TARGET_CREATED、WRITEBACK_PENDING、SUCCEEDED、FAILED |
| targetDocuments | 目标单据结果 |
| errorMessage | 错误信息 |

## 数据库通用字段

所有 BOTP 表使用 `matrix_botp_` 前缀，主键为 `fid`，字段统一以 `f` 开头，并包含租户、审计、逻辑删除和乐观锁字段。
