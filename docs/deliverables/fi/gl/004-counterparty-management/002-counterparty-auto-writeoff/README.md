# 往来自动核销交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/`
- 业务名称：往来自动核销
- 业务定位：在核销方案预览基础上的落库执行能力，用于批量生成自动核销日志和核销链接。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/test.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/review.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/ops.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/mock.md`

## 3. 交付物清单
- `dictionary.md`：执行字段与统计口径说明
- `api.md`：自动核销接口正式说明
- `sql.md`：预览与落库说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确方案号、日志号、核销链接与金额口径
2. 再看 `api.md` 与 `sql.md` 对齐预览接口、执行接口与落库逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现往来自动核销页面、方案预览与执行接口的场景
- 适用于自动核销落库、成功提示展示与方案页跳转验证

## 6. 风险与注意事项
- 当前执行会写入 `BizfiFiArapWriteoffLog / BizfiFiArapWriteoffLink`
- 方案号、日志号、链接数、核销总额、warnings 口径必须与后端实现一致
- 从方案页带入参数与执行接口映射必须保持一致
- 操作人、往来方等依赖需保持可用

## 7. 回滚与兜底
- 若自动核销口径或落库逻辑异常，优先回滚到已验证的执行逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若跳转或参数带入不兼容，先保留手工查询与执行能力，降级自动带入能力
