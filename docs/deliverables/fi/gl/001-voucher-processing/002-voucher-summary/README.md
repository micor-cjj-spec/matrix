# 凭证汇总表交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/001-voucher-processing/002-voucher-summary/`
- 业务名称：凭证汇总表
- 业务定位：总账统计分析页面，不直接制单，不直接维护分录。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/backend.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/sql.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/test.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/review.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/ops.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/mock.md`

## 3. 交付物清单
- `dictionary.md`：统计字段与口径说明
- `api.md`：汇总接口正式契约说明
- `sql.md`：聚合查询与索引建议
- `test.md`：测试用例与验证重点
- `ops.md`：发布、监控、回滚关注点
- `review.md`：一致性评审清单
- `mock.md`：请求响应与页面样例

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确统计口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与聚合逻辑
3. 然后执行 `test.md` 中的验证
4. 发布前检查 `ops.md` 和 `review.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现凭证汇总页面与汇总接口的场景
- 适用于汇总查询、告警展示、反查跳转验证

## 6. 风险与注意事项
- 当前统计基于凭证主表聚合，不读取分录表
- warnings 口径必须与后端实现保持一致
- 反查跳转参数需要和凭证列表页过滤条件口径一致
- 当前页面不支持导出，交付物不应虚构导出逻辑

## 7. 回滚与兜底
- 若统计口径调整失败，优先回滚到已验证的聚合逻辑
- 若 warnings 规则存在争议，先维持页面展示但标记说明口径待确认
- 若跳转参数不兼容，先保留汇总浏览能力，暂停反查跳转联动
