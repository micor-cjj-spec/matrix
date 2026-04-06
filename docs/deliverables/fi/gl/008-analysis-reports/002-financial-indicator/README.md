# 财务指标交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/008-analysis-reports/002-financial-indicator/`
- 业务名称：财务指标
- 业务定位：当前仅为总账首页菜单占位，尚未形成可执行业务能力。

## 2. 与代码对齐结论
根据现有 BUS 说明：
- 总账首页未配置独立 `path`
- 前端未发现已接入的独立路由页面
- 后端未发现对应 controller / service 实现

因此，本交付物仅保留占位说明，不展开脱离代码事实的业务细节。

## 3. 对应 Prompt 来源
- `docs/prompt/fi/gl/008-analysis-reports/002-financial-indicator/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/002-financial-indicator/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/002-financial-indicator/backend.md`
- `docs/prompt/fi/gl/008-analysis-reports/002-financial-indicator/frontend.md`
- `docs/prompt/fi/gl/008-analysis-reports/002-financial-indicator/sql.md`
- `docs/prompt/fi/gl/008-analysis-reports/002-financial-indicator/test.md`
- `docs/prompt/fi/gl/008-analysis-reports/002-financial-indicator/review.md`
- `docs/prompt/fi/gl/008-analysis-reports/002-financial-indicator/ops.md`
- `docs/prompt/fi/gl/008-analysis-reports/002-financial-indicator/mock.md`

## 4. 当前交付物定位
- 当前用于记录“未实现状态”和“后续补齐前提”
- 不用于指导实际开发落地、联调或测试执行

## 5. 后续补齐前提
待以下任一内容落地后，再将本占位交付物升级为正式交付物：
1. 前端页面/交互实现
2. 后端接口与服务实现
3. 指标定义与计算实现

## 6. 风险与注意事项
- 当前不得虚构正式接口、正式页面、正式表结构或正式计算流程
- 后续若正式落地，应先补齐 BUS 文档，再反向修订 prompt 和交付物
