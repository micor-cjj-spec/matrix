# 会计期间前端开发提示词

## 目标
设计会计期间的列表、详情、编辑页面，并与现有后端接口保持一致。

## 必须参考的文档和代码
- `docs/bus/fi/gl/accounting-period/*.md`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiAccountingPeriodController.java`
- `fi-service/src/main/java/single/cjj/fi/gl/service/impl/BizfiFiAccountingPeriodServiceImpl.java`

## 页面目标
1. 支持期间列表分页查询
2. 支持新增、编辑、删除
3. 支持关账、反开操作
4. 支持按组织、状态、年度、期间筛选

## 设计要求
1. `fperiod` 使用 `yyyy-MM` 格式输入。
2. `fstatus` 按 `OPEN / CLOSED` 枚举展示。
3. 关账和反开按钮严格按状态显隐。
4. 列表与详情要展示关账人、关账时间。

## 输出要求
1. 页面结构
2. 表单字段设计
3. 查询条件设计
4. 按钮状态控制表
5. 接口调用方案
