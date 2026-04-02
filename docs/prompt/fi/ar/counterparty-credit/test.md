# 往来方信用配置测试提示词

## 目标
针对信用配置的保存、查询、默认值处理、预警命中和硬拦截逻辑输出测试方案。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/ar/entity/BizfiFiCounterpartyCredit.java`
- `fi-service/src/main/java/single/cjj/fi/ar/controller/BizfiFiArapDocController.java`
- `fi-service/src/main/java/single/cjj/fi/ar/service/impl/BizfiFiArapDocServiceImpl.java`

## 测试重点
1. 新增信用配置
2. 按 `counterparty + docTypeRoot` 更新已有配置
3. 额度、逾期阈值、启用标识默认值处理
4. 预警查询结果
5. 提交/审核时命中硬拦截的异常场景
6. AR 与 AP 两类根类型的隔离验证

## 输出要求
1. 测试范围
2. 正常场景用例
3. 异常场景用例
4. 默认值与边界值用例
5. 拦截规则用例
6. 接口回归清单
