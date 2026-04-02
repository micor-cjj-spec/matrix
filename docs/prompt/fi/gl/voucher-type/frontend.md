# 凭证字前端开发提示词

## 目标
设计凭证字配置页面，并与现有后端接口保持一致。

## 必须参考的文档和代码
- `docs/bus/fi/gl/voucher-type/*.md`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherTypeController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiVoucherTypeServiceImpl.java`

## 页面目标
1. 支持列表分页查询
2. 支持新增、编辑、删除
3. 支持启用、停用
4. 支持按组织、状态、编码、名称筛选

## 设计要求
1. `fcode` 前端输入后可自动转大写展示。
2. 状态使用 `ENABLED / DISABLED` 枚举。
3. 列表页要突出默认使用频率高的字段：编码、名称、前缀、排序、状态。
4. 启用/停用按钮按当前状态动态控制。

## 输出要求
1. 页面结构
2. 表单字段设计
3. 列表字段设计
4. 按钮状态控制表
5. 接口调用方案
