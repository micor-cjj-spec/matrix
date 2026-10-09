# 往来账查询样例说明

## 1. 查询请求样例
```json
{
  "docTypeRoot": "AR",
  "counterparty": "CP001",
  "docType": "INVOICE",
  "status": "AUDITED",
  "openOnly": true,
  "keyword": "差旅",
  "asOfDate": "2026-04-30"
}
```

## 2. 返回样例
```json
{
  "docCount": 12,
  "totalAmount": 320000.00,
  "writtenOffAmount": 280000.00,
  "openAmount": 40000.00,
  "openDocCount": 3,
  "warnings": [
    "存在部分核销单据，请关注剩余未核销金额。"
  ],
  "records": [
    {
      "docNo": "AR202604001",
      "docType": "INVOICE",
      "status": "AUDITED",
      "totalAmount": 60000.00,
      "writtenOffAmount": 50000.00,
      "openAmount": 10000.00,
      "writeoffStatus": "PARTIAL",
      "agingDays": 30
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "docCount": 0,
  "totalAmount": 0,
  "writtenOffAmount": 0,
  "openAmount": 0,
  "openDocCount": 0,
  "warnings": [],
  "records": []
}
```

## 4. 关联凭证查看样例
- 单据号：`docNo`
- 查看口径：基于当前行展示或跳转关联凭证信息

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
