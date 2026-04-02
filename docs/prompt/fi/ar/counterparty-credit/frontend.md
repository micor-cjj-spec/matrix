# 往来方信用配置前端开发提示词

## 目标
设计往来方信用配置维护页面和预警查看页面，前端行为要严格匹配现有后端接口与字段。

## 必须参考的文档和代码
- `docs/bus/fi/ar/counterparty-credit/business.md`
- `docs/bus/fi/ar/counterparty-credit/fields.md`
- `fi-service/src/main/java/single/cjj/fi/ar/entity/BizfiFiCounterpartyCredit.java`
- `fi-service/src/main/java/single/cjj/fi/ar/controller/BizfiFiArapDocController.java`

## 页面目标
1. 提供信用配置列表和编辑表单
2. 支持按 AR/AP 根类型过滤配置
3. 支持维护额度、逾期阈值、启用状态、硬拦截开关
4. 提供信用预警结果查看页面

## 设计要求
1. 表单字段命名和后端保持一致。
2. `AR / AP` 根类型使用下拉枚举，不允许自由输入。
3. 对额度、逾期阈值等字段做前端校验。
4. 配置页和预警页要区分维护视图与分析视图。

## 输出要求
1. 页面结构
2. 表单字段设计
3. 列表字段设计
4. 接口调用方案
5. 校验规则说明
