# 凭证分析测试提示词

## 目标
围绕现有凭证分析实现，输出覆盖汇总分析、结转清单、参数解析和警告信息的测试方案。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiVoucherAnalysisServiceImpl.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherController.java`

## 测试重点
1. 日期范围参数合法性
2. 状态条件规范化
3. 摘要关键字过滤
4. 无数据时的警告返回
5. 结转清单期间解析和回退逻辑
6. 默认凭证字缺失、期间缺失、配置缺失时的任务提示
7. 结转凭证关键字识别

## 输出要求
1. 测试范围
2. 正常场景用例
3. 异常场景用例
4. 警告信息用例
5. 结转清单用例
6. 接口回归清单
