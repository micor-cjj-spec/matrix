# 结转清单交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/001-voucher-processing/003-carry-list/`
- 业务名称：结转清单
- 业务定位：期末处理前的检查与提示页面，不直接生成结转凭证，不直接执行结账。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/backend.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/sql.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/test.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/review.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/ops.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/mock.md`

## 3. 交付物清单
- `dictionary.md`：检查项、元信息、warnings、相关凭证口径说明
- `api.md`：结转清单接口正式说明
- `sql.md`：查询与聚合说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确检查与统计口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现结转清单页面与结转清单接口的场景
- 适用于期末处理前检查、导航、相关凭证复核

## 6. 风险与注意事项
- 当前场景只做检查与导航，不应扩展为直接执行结转或结账
- warnings 口径、基础资料健康度口径必须与后端实现一致
- 查看凭证跳转参数需要与凭证页查询口径保持一致
- 业务单元列表初始化依赖需保持可用

## 7. 回滚与兜底
- 若检查口径调整失败，优先回滚到已验证的聚合逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若跳转参数不兼容，先保留检查与浏览能力，暂停反查跳转联动
