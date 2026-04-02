# 往来单测试提示词

## 目标
基于现有统一往来单实现，输出覆盖状态流转、信用控制、凭证联动和分析查询的测试方案。

## 必须参考的代码与文档
- `docs/bus/fi/ar/arap-doc/*.md`
- `fi-service/src/main/java/single/cjj/fi/ar/entity/BizfiFiArapDoc.java`
- `fi-service/src/main/java/single/cjj/fi/ar/controller/BizfiFiArapDocController.java`
- `fi-service/src/main/java/single/cjj/fi/ar/service/impl/BizfiFiArapDocServiceImpl.java`

## 测试重点
1. 创建、修改、删除草稿
2. 草稿/驳回提交
3. 已提交审核、驳回
4. 审核通过自动生成凭证
5. 重复审核或重复生成凭证的幂等性
6. 信用额度和逾期硬拦截
7. 账龄汇总结果分桶
8. 按凭证反查来源单据

## 输出要求
请输出：
1. 测试范围
2. 正常场景用例
3. 异常场景用例
4. 状态机用例
5. 联动凭证用例
6. 信用控制用例
7. 分析查询用例
