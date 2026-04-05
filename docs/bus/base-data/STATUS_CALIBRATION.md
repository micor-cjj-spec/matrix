# 基础资料模块状态校准（base-data）

> 基于 `dev` 分支当前已落文档结果进行的阶段性校准。

## 一、当前已建立模块
- `001-material`
- `002-customer`
- `003-supplier`
- `004-currency`
- `005-exchange-rate`
- `006-bank-info`
- `007-country`
- `008-region`
- `009-unit`

## 二、当前状态校准
### 1. 已基本完整的模块
以下模块已具备：
- `manifest.json`
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

当前归入此类的模块：
- `001-material`
- `002-customer`
- `003-supplier`
- `004-currency`
- `005-exchange-rate`
- `006-bank-info`
- `007-country`
- `008-region`
- `009-unit`

### 2. 状态口径
尽管文档结构已接近完整，但当前模块仍建议统一视为：
- `partial_frontend_integrated`

原因：
1. 前端页面、路由和接口调用已确认。
2. 通用前端行为、状态流和交互规则已确认。
3. 本轮未完整定位到对应后端 controller / service 实现文件，因此不直接上调为完全代码闭环状态。

## 三、统一代码模式
当前基础资料这 9 个模块都采用相同前端模式：
1. 真实前端页面，不是占位页。
2. 统一复用：`useSimpleData.js`
3. 统一支持：
   - 列表
   - 创建
   - 编辑
   - 删除
   - 提交审核
   - 审核通过
   - 驳回
4. 统一状态流：
   - `DRAFT`
   - `SUBMITTED`
   - `AUDITED`
   - `REJECTED`
5. 统一页面规则：
   - 名称必填
   - 编码可选
   - `DRAFT` 可提交
   - `SUBMITTED` 可审核或驳回
   - `AUDITED` 不允许编辑和删除

## 四、当前建议
1. 后续若确认后端实现类，可把这 9 个模块统一提升为更高完成状态。
2. 在具备可稳定更新现有文件能力后，应将本文件中的结论回写到 `AUDIT_STATUS.md`。
3. 当前阅读优先级：
   - `INDEX.md`
   - `HANDOFF.md`
   - `STATUS_CALIBRATION.md`
