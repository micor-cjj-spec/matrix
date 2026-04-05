# 凭证汇总表样例提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/002-voucher-summary/` 下业务文档，以及当前已确认实现，输出凭证汇总表场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/test.md`

## 输出要求
1. 输出默认查询样例、汇总卡片样例、warnings 样例、表格行样例
2. 输出带状态过滤、摘要关键字过滤、空结果、异常结果等样例
3. 输出“查看凭证”跳转参数样例
4. 输出接口请求与返回样例
5. 对 BUS 未明确项显式标记“待确认”
