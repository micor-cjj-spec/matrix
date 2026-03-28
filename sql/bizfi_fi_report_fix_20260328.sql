-- 修复报表种子中文乱码 + 基础报表映射

UPDATE bizfi_fi_report_template SET fname='资产负债表（标准版）' WHERE fid=1001;
UPDATE bizfi_fi_report_template SET fname='利润表（标准版）' WHERE fid=1002;
UPDATE bizfi_fi_report_template SET fname='现金流量表（标准版）' WHERE fid=1003;

UPDATE bizfi_fi_report_item SET fname='资产' WHERE fid=1101;
UPDATE bizfi_fi_report_item SET fname='货币资金' WHERE fid=1102;
UPDATE bizfi_fi_report_item SET fname='负债和所有者权益' WHERE fid=1103;
UPDATE bizfi_fi_report_item SET fname='营业收入' WHERE fid=1201;
UPDATE bizfi_fi_report_item SET fname='营业成本' WHERE fid=1202;
UPDATE bizfi_fi_report_item SET fname='净利润' WHERE fid=1203;
UPDATE bizfi_fi_report_item SET fname='经营活动现金流量净额' WHERE fid=1301;
UPDATE bizfi_fi_report_item SET fname='投资活动现金流量净额' WHERE fid=1302;
UPDATE bizfi_fi_report_item SET fname='筹资活动现金流量净额' WHERE fid=1303;
UPDATE bizfi_fi_report_item SET fname='现金及现金等价物净增加额' WHERE fid=1304;

UPDATE bizfi_fi_cashflow_item SET fname='经营活动现金流项目' WHERE fid=2001;
UPDATE bizfi_fi_cashflow_item SET fname='投资活动现金流项目' WHERE fid=2002;
UPDATE bizfi_fi_cashflow_item SET fname='筹资活动现金流项目' WHERE fid=2003;

-- 基础映射：资产负债表
INSERT INTO bizfi_fi_report_account_map
(fid, ftemplate_id, fitem_id, faccount_id, fmapping_type, fremark)
VALUES
(900001, 1001, 1102, 1, 'DIRECT', 'seed fix: 1001 库存现金 -> 货币资金'),
(900002, 1001, 1103, 4, 'DIRECT', 'seed fix: 2001 短期借款 -> 负债和所有者权益')
ON DUPLICATE KEY UPDATE
fitem_id=VALUES(fitem_id),
faccount_id=VALUES(faccount_id),
fmapping_type=VALUES(fmapping_type),
fremark=VALUES(fremark),
fupdatetime=CURRENT_TIMESTAMP;
