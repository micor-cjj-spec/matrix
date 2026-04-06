# 往来通知单口径说明

## 1. 查询与生成参数口径
- `docTypeRoot`：往来类型
- `status`：通知状态
- `severity`：紧急程度
- `asOfDate`：统计日期
- `operator`：生成接口可选操作人

## 2. 汇总统计口径
- `noticeCount`：通知数
- `openCount`：未关闭通知数
- `resolvedCount`：已关闭通知数
- `highCount`：高优先级通知数
- `amount`：通知金额
- `openAmount`：未清金额

## 3. 生成结果口径
- `generatedCount`：本次生成通知总数
- `insertedCount`：新增通知数
- `updatedCount`：更新通知数
- `resolvedAutoCount`：自动关闭通知数
- `message`：生成成功提示

## 4. 通知清单口径
- `rows`：展示通知状态、紧急程度、处理建议、快照金额、未清金额等信息
- 行内可跳转到往来对账单或通知单勾稽页面

## 5. warnings 口径
- warnings 由后端返回
- 用于提示生成范围、异常通知或需要重点关注的问题

## 6. 待确认项
- 通知状态枚举是否需要独立文档化
- 自动关闭的最终触发条件是否需要更细说明
