-- 2026-03-29 PROD cleanup: overwrite garbled report seed labels in bizfi_fi
-- Safe to run repeatedly.

USE bizfi_fi;
SET NAMES utf8mb4;

UPDATE bizfi_fi_report_template SET fname='资产负债表（标准版）' WHERE fid=1001 AND fcode='BS-STD';
UPDATE bizfi_fi_report_template SET fname='利润表（标准版）' WHERE fid=1002 AND fcode='PL-STD';
UPDATE bizfi_fi_report_template SET fname='现金流量表（标准版）' WHERE fid=1003 AND fcode='CF-STD';

UPDATE bizfi_fi_report_item SET fname='资产' WHERE fid=1101 AND fcode='BS_ASSET';
UPDATE bizfi_fi_report_item SET fname='货币资金' WHERE fid=1102 AND fcode='BS_CASH';
UPDATE bizfi_fi_report_item SET fname='负债和所有者权益' WHERE fid=1103 AND fcode='BS_LIAB_EQ';
UPDATE bizfi_fi_report_item SET fname='营业收入' WHERE fid=1201 AND fcode='PL_REVENUE';
UPDATE bizfi_fi_report_item SET fname='营业成本' WHERE fid=1202 AND fcode='PL_COST';
UPDATE bizfi_fi_report_item SET fname='净利润' WHERE fid=1203 AND fcode='PL_NET_PROFIT';
UPDATE bizfi_fi_report_item SET fname='经营活动现金流量净额' WHERE fid=1301 AND fcode='CF_OPERATING';
UPDATE bizfi_fi_report_item SET fname='投资活动现金流量净额' WHERE fid=1302 AND fcode='CF_INVESTING';
UPDATE bizfi_fi_report_item SET fname='筹资活动现金流量净额' WHERE fid=1303 AND fcode='CF_FINANCING';
UPDATE bizfi_fi_report_item SET fname='现金及现金等价物净增加额' WHERE fid=1304 AND fcode='CF_NET_INCREASE';

UPDATE bizfi_fi_cashflow_item SET fname='经营活动现金流项目' WHERE fid=2001 AND fcode='CF_OPERATING';
UPDATE bizfi_fi_cashflow_item SET fname='投资活动现金流项目' WHERE fid=2002 AND fcode='CF_INVESTING';
UPDATE bizfi_fi_cashflow_item SET fname='筹资活动现金流项目' WHERE fid=2003 AND fcode='CF_FINANCING';

-- Keep essential balance-sheet mappings in place
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
