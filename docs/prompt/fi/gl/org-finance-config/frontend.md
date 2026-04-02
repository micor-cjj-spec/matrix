# 组织财务配置前端开发提示词

## 目标
设计组织财务配置页面，并与现有接口和字段保持一致。

## 必须参考的文档和代码
- `docs/bus/fi/gl/org-finance-config/*.md`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiOrgFinanceConfigController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiOrgFinanceConfigServiceImpl.java`

## 页面目标
1. 支持按组织查询配置
2. 支持新增、编辑、删除
3. 支持列表分页查询
4. 支持展示当前期间、本位币、默认凭证字、期间控制模式、状态

## 设计要求
1. `fbaseCurrency` 建议使用大写币种编码。
2. `fcurrentPeriod` 必须按 `yyyy-MM` 输入。
3. `fperiodControlMode` 和 `fstatus` 使用枚举下拉。
4. 列表页优先展示组织、本位币、当前期间、默认凭证字、状态。

## 输出要求
1. 页面结构
2. 表单字段设计
3. 查询条件设计
4. 校验规则说明
5. 接口调用方案
