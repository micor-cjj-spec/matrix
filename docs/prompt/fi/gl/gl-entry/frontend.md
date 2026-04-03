# 总账分录前端开发提示词

## 目标
设计总账分录查询和追溯页面，不提供人工编辑入口。

## 必须参考的文档和代码
- `docs/bus/fi/gl/gl-entry/*.md`
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiGlEntry.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiVoucherServiceImpl.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherController.java`

## 页面目标
1. 支持总账分录列表查询
2. 支持查看来源凭证和来源凭证行
3. 支持按凭证号、日期、科目、过账人筛选
4. 支持从凭证详情跳转查看总账分录结果

## 设计要求
1. 页面以查询和追溯为主，不提供新增/编辑按钮。
2. 列表需要突出借贷金额、科目、过账时间、过账人。
3. 支持跳转到来源凭证详情。
4. 若当前后端尚无独立查询接口，先给出接口设计建议和页面占位方案。

## 输出要求
1. 页面结构
2. 列表字段设计
3. 查询条件设计
4. 跳转与追溯关系设计
5. 接口调用或接口建议
