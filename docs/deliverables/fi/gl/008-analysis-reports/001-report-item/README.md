# 报表项目交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/008-analysis-reports/001-report-item/`
- 业务名称：报表项目
- 业务定位：用于维护三大报表的项目定义、层级、行次、行类型和可下钻属性，是报表模板的基础配置数据。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/backend.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/frontend.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/sql.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/test.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/review.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/ops.md`
- `docs/prompt/fi/gl/008-analysis-reports/001-report-item/mock.md`

## 3. 交付物清单
- `dictionary.md`：报表项目字段与层级口径说明
- `api.md`：报表项目接口正式说明
- `sql.md`：查询与树结构说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确模板、项目编码、项目名称、行次、层级、行类型、期间模式、可下钻、排序口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现报表项目页面与 `/report-item/*` 接口场景
- 适用于报表模板基础配置、项目树维护与项目查询展示

## 6. 风险与注意事项
- 当前页面仅接入查询、刷新与分页展示，未接入新增、编辑、删除入口
- 后端已提供写接口，但前端未接入，需避免误判为前端已开放维护能力
- 模板与项目树层级一致性必须与后端校验保持一致

## 7. 回滚与兜底
- 若列表查询或模板加载异常，优先回滚到已验证的查询逻辑
- 若树结构或排序口径存在争议，先保留分页列表能力，树能力单独校验
- 若前端计划开放写入口，应先补齐权限、校验与测试文档
