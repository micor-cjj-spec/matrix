# 凭证分析前端开发提示词

## 目标
设计凭证汇总分析页和结转清单页，并与现有分析接口保持一致。

## 必须参考的文档和代码
- `docs/bus/fi/gl/voucher-analysis/*.md`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiVoucherAnalysisServiceImpl.java`

## 页面目标
1. 支持凭证汇总分析页
2. 支持结转清单页
3. 支持展示警告信息、任务项、统计结果
4. 支持按日期、状态、组织、期间等条件筛选

## 设计要求
1. 页面以查询和分析展示为主，不提供修改按钮。
2. 汇总分析页突出总笔数、总金额、状态分布和日期行汇总。
3. 结转清单页突出待处理任务、默认凭证字、当前期间、相关结转凭证。
4. 警告信息要做明显提示。

## 输出要求
1. 页面结构
2. 查询条件设计
3. 结果区块设计
4. 告警展示设计
5. 接口调用方案
