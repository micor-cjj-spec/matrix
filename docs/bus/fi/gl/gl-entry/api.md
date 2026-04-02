# 接口说明

当前代码中没有看到独立的总账分录 Controller；总账分录主要通过凭证过账和冲销流程间接维护。fileciteturn217file0L88-L125 fileciteturn217file0L176-L227

## 间接相关接口
- `POST /voucher/post/{fid}`：单笔过账，生成总账分录。fileciteturn179file0L57-L60
- `POST /voucher/post/batch`：批量过账，批量生成总账分录。fileciteturn179file0L62-L67
- `POST /voucher/reverse/{fid}`：冲销凭证并生成反向总账分录。fileciteturn179file0L74-L77

## 说明
- 若后续需要独立总账分录查询接口，可以基于 `BizfiFiGlEntry` 和 `BizfiFiGlEntryMapper` 补充查询 Controller / Service。
