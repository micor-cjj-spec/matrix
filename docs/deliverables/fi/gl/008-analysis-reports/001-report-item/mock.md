# 报表项目样例说明

## 1. 查询请求样例
```json
{
  "templateId": 1,
  "itemCode": "BS_1001",
  "itemName": "货币资金",
  "pageNum": 1,
  "pageSize": 20
}
```

## 2. 分页返回样例
```json
{
  "total": 2,
  "records": [
    {
      "templateName": "资产负债表",
      "itemCode": "BS_1001",
      "itemName": "货币资金",
      "rowNo": 1,
      "level": 1,
      "rowType": "NORMAL",
      "periodMode": "BALANCE",
      "drillable": true,
      "sortNo": 10
    }
  ]
}
```

## 3. 树查询样例
```json
[
  {
    "fid": 1,
    "itemCode": "BS_1000",
    "itemName": "资产",
    "children": [
      {
        "fid": 2,
        "itemCode": "BS_1001",
        "itemName": "货币资金"
      }
    ]
  }
]
```

## 4. 空结果样例
```json
{
  "total": 0,
  "records": []
}
```

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 写接口样例当前不在页面显式使用范围内
- 若返回结构扩展，应同步更新本文件
