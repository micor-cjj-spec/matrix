# 凭证分析后端开发提示词

## 目标
基于现有 `BizfiFiVoucherAnalysisServiceImpl` 继续增强凭证汇总分析和结转清单能力，不重新起一套平行分析模块。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiVoucherAnalysisServiceImpl.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiVoucher.java`
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiAccountingPeriod.java`
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiVoucherType.java`
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiOrgFinanceConfig.java`

## 当前代码事实
1. 已支持凭证汇总分析 `summary`。
2. 已支持结转清单分析 `carryList`。
3. 分析过程中会读取组织财务配置、会计期间、默认凭证字和基础资料检查结果。
4. 结转相关凭证通过摘要或备注关键字识别。

## 开发要求
1. 保持分析能力只读，不修改业务数据。
2. 继续复用现有分析服务，不重新拆模块。
3. 涉及日期、期间、状态解析时保持当前兼容策略。
4. 新增分析项时尽量输出结构化结果和警告信息。
5. 输出代码时列出修改文件路径。

## 可扩展方向
- 增加更多汇总维度
- 增加结转清单任务细化
- 增加图表友好的统计结果
- 增加更多基础资料检查联动

## 输出格式要求
1. 改造目标
2. 影响类清单
3. 关键代码
4. 风险点
5. 文件路径清单
