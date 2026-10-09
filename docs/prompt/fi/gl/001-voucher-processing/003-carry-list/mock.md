# 结转清单样例提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/003-carry-list/` 下业务文档，以及当前已确认实现，输出结转清单场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/test.md`

## 输出要求
1. 输出默认查询样例、元信息样例、检查项样例、相关凭证样例
2. 输出 warnings 样例、空结果样例、异常样例
3. 输出查看凭证跳转参数样例
4. 输出接口请求与返回样例
5. 对 BUS 未明确项显式标记“待确认”
