CREATE DATABASE IF NOT EXISTS matrix_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE matrix_platform;

CREATE TABLE IF NOT EXISTS matrix_platform_app (
    fid BIGINT NOT NULL COMMENT '主键',
    fapp_code VARCHAR(64) NOT NULL COMMENT '应用编码',
    fname VARCHAR(128) NOT NULL COMMENT '应用名称',
    fdescription VARCHAR(500) NULL COMMENT '应用说明',
    fmeta VARCHAR(128) NULL COMMENT '应用辅助信息',
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '数据状态',
    fstatus_text VARCHAR(64) NULL COMMENT '前台状态文案',
    froute_path VARCHAR(255) NULL COMMENT '前端路由',
    ficon_key VARCHAR(64) NULL COMMENT '前端图标编码',
    faccent VARCHAR(32) NULL COMMENT '主题色',
    ffeatured TINYINT NOT NULL DEFAULT 0 COMMENT '是否重点应用',
    favailable TINYINT NOT NULL DEFAULT 1 COMMENT '是否可用',
    fnew_page TINYINT NOT NULL DEFAULT 0 COMMENT '是否新页面打开',
    fsort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    fcreate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    fmodify_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_platform_app_code (fapp_code),
    KEY idx_matrix_platform_app_sort (fsort_no)
) COMMENT='Matrix平台应用入口';

CREATE TABLE IF NOT EXISTS matrix_platform_menu (
    fid BIGINT NOT NULL COMMENT '主键',
    fparent_id BIGINT NULL COMMENT '父级ID',
    fapp_code VARCHAR(64) NOT NULL COMMENT '应用编码',
    fmodule_code VARCHAR(64) NULL COMMENT '模块编码',
    fmenu_code VARCHAR(128) NOT NULL COMMENT '菜单编码',
    fname VARCHAR(128) NOT NULL COMMENT '菜单名称',
    fdescription VARCHAR(500) NULL COMMENT '菜单说明',
    fsummary VARCHAR(255) NULL COMMENT '分组摘要',
    feyebrow VARCHAR(64) NULL COMMENT '英文标识',
    fmenu_type VARCHAR(32) NOT NULL COMMENT '菜单类型',
    froute_path VARCHAR(255) NULL COMMENT '前端路由',
    ficon_key VARCHAR(64) NULL COMMENT '前端图标编码',
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '数据状态',
    fstatus_text VARCHAR(64) NULL COMMENT '前台状态文案',
    favailable TINYINT NOT NULL DEFAULT 1 COMMENT '是否可用',
    fsort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    fmeta_json JSON NULL COMMENT '扩展元数据',
    fcreate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    fmodify_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (fid),
    UNIQUE KEY uk_matrix_platform_menu_code (fapp_code, fmenu_code),
    KEY idx_matrix_platform_menu_parent (fapp_code, fparent_id),
    KEY idx_matrix_platform_menu_module (fapp_code, fmodule_code, fmenu_type, fsort_no)
) COMMENT='Matrix平台菜单配置';

CREATE TABLE IF NOT EXISTS matrix_platform_workbench_item (
    fid BIGINT NOT NULL COMMENT '主键',
    fsection VARCHAR(64) NOT NULL COMMENT '工作台区块',
    fname VARCHAR(128) NOT NULL COMMENT '项目名称',
    fdescription VARCHAR(500) NULL COMMENT '项目说明',
    fvalue VARCHAR(128) NULL COMMENT '展示值',
    fhint VARCHAR(255) NULL COMMENT '提示信息',
    ftag VARCHAR(64) NULL COMMENT '标签',
    fitem_type VARCHAR(64) NULL COMMENT '项目类型',
    fpriority VARCHAR(32) NULL COMMENT '优先级',
    froute_path VARCHAR(255) NULL COMMENT '前端路由',
    ficon_key VARCHAR(64) NULL COMMENT '前端图标编码',
    faccent VARCHAR(32) NULL COMMENT '主题色',
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '数据状态',
    fstatus_text VARCHAR(64) NULL COMMENT '前台状态文案',
    favailable TINYINT NOT NULL DEFAULT 1 COMMENT '是否可用',
    fnew_page TINYINT NOT NULL DEFAULT 0 COMMENT '是否新页面打开',
    ffeatured TINYINT NOT NULL DEFAULT 0 COMMENT '是否重点项',
    fsort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    fmeta_json JSON NULL COMMENT '扩展元数据',
    fcreate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    fmodify_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (fid),
    KEY idx_matrix_platform_workbench_section (fsection, fstatus, fsort_no)
) COMMENT='Matrix个人工作台配置项';

CREATE TABLE IF NOT EXISTS matrix_platform_module_item (
    fid BIGINT NOT NULL COMMENT '主键',
    fapp_code VARCHAR(64) NOT NULL COMMENT '应用编码',
    fmodule_code VARCHAR(64) NOT NULL COMMENT '模块编码',
    fsection VARCHAR(64) NOT NULL COMMENT '模块区块',
    fname VARCHAR(128) NOT NULL COMMENT '项目名称',
    fdescription VARCHAR(500) NULL COMMENT '项目说明',
    fvalue VARCHAR(128) NULL COMMENT '展示值',
    fhint VARCHAR(255) NULL COMMENT '提示信息',
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '数据状态',
    fstatus_text VARCHAR(64) NULL COMMENT '前台状态文案',
    froute_path VARCHAR(255) NULL COMMENT '前端路由',
    ficon_key VARCHAR(64) NULL COMMENT '前端图标编码',
    fprimary_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否主按钮',
    fsort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    fmeta_json JSON NULL COMMENT '扩展元数据',
    fcreate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    fmodify_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (fid),
    KEY idx_matrix_platform_module_item (fapp_code, fmodule_code, fsection, fstatus, fsort_no)
) COMMENT='Matrix模块首页配置项';

INSERT INTO matrix_platform_app
(fid, fapp_code, fname, fdescription, fmeta, fstatus, fstatus_text, froute_path, ficon_key, faccent, ffeatured, favailable, fnew_page, fsort_no)
VALUES
(1001, 'finance', '财务系统', '总账、应收应付、报表、月结的统一入口。', '核心系统', 'ENABLED', '已上线', '/finance', 'Wallet', '#0f8a6a', 1, 1, 1, 10),
(1002, 'knowledge', '知识系统', '沉淀制度、流程、案例与业务问答，连接 AI 助手。', '知识底座', 'ENABLED', '已上线', '/ai/knowledge', 'Notebook', '#1769aa', 0, 1, 0, 20),
(1003, 'approval', '审批系统', '费用、付款、合同与组织审批流的统一处理中心。', '流程能力', 'ENABLED', '规划中', NULL, 'DocumentChecked', '#b7791f', 0, 0, 0, 30),
(1004, 'project', '项目系统', '项目预算、成本归集、里程碑与经营分析。', '业务协同', 'ENABLED', '规划中', NULL, 'Briefcase', '#6f7b35', 0, 0, 0, 40),
(1005, 'ai', 'AI 助手', '围绕财务、数据与知识的智能问答入口。', '智能协作', 'ENABLED', '已上线', '/ai/assistant', 'ChatDotRound', '#2454a6', 0, 1, 0, 50),
(1006, 'base', '基础服务', '企业建模、主数据、人员与组织基础能力。', '平台底座', 'ENABLED', '已上线', '/base-data', 'OfficeBuilding', '#56636f', 0, 1, 0, 60)
ON DUPLICATE KEY UPDATE
fname = VALUES(fname), fdescription = VALUES(fdescription), fmeta = VALUES(fmeta), fstatus = VALUES(fstatus),
fstatus_text = VALUES(fstatus_text), froute_path = VALUES(froute_path), ficon_key = VALUES(ficon_key),
faccent = VALUES(faccent), ffeatured = VALUES(ffeatured), favailable = VALUES(favailable),
fnew_page = VALUES(fnew_page), fsort_no = VALUES(fsort_no);

INSERT INTO matrix_platform_workbench_item
(fid, fsection, fname, fdescription, fvalue, fhint, ftag, fitem_type, fpriority, froute_path, ficon_key, faccent, fstatus, fstatus_text, favailable, fnew_page, ffeatured, fsort_no)
VALUES
(2001, 'HERO_METRIC', '月结进度', NULL, '82%', '较昨日 +11%', NULL, NULL, NULL, NULL, NULL, NULL, 'ENABLED', NULL, 1, 0, 0, 10),
(2002, 'HERO_METRIC', '待我处理', NULL, '6', '含 2 项高优先级', NULL, NULL, NULL, NULL, NULL, NULL, 'ENABLED', NULL, 1, 0, 0, 20),
(2003, 'HERO_METRIC', '本月凭证', NULL, '1,280', '自动生成 64%', NULL, NULL, NULL, NULL, NULL, NULL, 'ENABLED', NULL, 1, 0, 0, 30),
(2101, 'TODO', '确认 4 月月结检查项', '总账模块还有 3 项需确认', NULL, NULL, NULL, NULL, 'high', '/ledger/month-end-close-workbench', NULL, NULL, 'ENABLED', NULL, 1, 0, 0, 10),
(2102, 'TODO', '复核应付账龄预警', '2 家供应商超过信用期', NULL, NULL, NULL, NULL, 'medium', '/payable/aging-credit', NULL, NULL, 'ENABLED', NULL, 1, 0, 0, 20),
(2103, 'TODO', '补充现金流通知单', '经营活动现金流待勾稽', NULL, NULL, NULL, NULL, 'low', '/ledger/cashflow-notice-check', NULL, NULL, 'ENABLED', NULL, 1, 0, 0, 30),
(2201, 'RECENT', '资产负债表', '2026 年 4 月报表', '10:24', NULL, NULL, NULL, NULL, '/ledger/balance-sheet', 'DataAnalysis', NULL, 'ENABLED', NULL, 1, 0, 0, 10),
(2202, 'RECENT', '凭证协同检查', '自动生成凭证复核', '昨天', NULL, NULL, NULL, NULL, '/ledger/voucher-collaboration-check', 'Tickets', NULL, 'ENABLED', NULL, 1, 0, 0, 20),
(2203, 'RECENT', '往来对账单', '客户与供应商余额核对', '周五', NULL, NULL, NULL, NULL, '/ledger/counterparty-statement', 'Files', NULL, 'ENABLED', NULL, 1, 0, 0, 30),
(2301, 'NOTICE', '月结监控中心已更新', '新增异常凭证定位与结转进度提醒。', NULL, NULL, '财务', 'finance', NULL, NULL, NULL, NULL, 'ENABLED', NULL, 1, 0, 0, 10),
(2302, 'NOTICE', 'Matrix 工作台升级', '多系统入口、待办与最近访问已整合。', NULL, NULL, '平台', 'platform', NULL, NULL, NULL, NULL, 'ENABLED', NULL, 1, 0, 0, 20),
(2303, 'NOTICE', '知识系统进入产品设计', '后续会接入制度、问答与文档资产。', NULL, NULL, '知识', 'knowledge', NULL, NULL, NULL, NULL, 'ENABLED', NULL, 1, 0, 0, 30),
(2401, 'QUICK_ACTION', '新增凭证', NULL, NULL, NULL, NULL, NULL, NULL, '/ledger/voucher', 'Plus', NULL, 'ENABLED', NULL, 1, 0, 0, 10),
(2402, 'QUICK_ACTION', '上传附件', NULL, NULL, NULL, NULL, NULL, NULL, '/payable/manage', 'Upload', NULL, 'ENABLED', NULL, 1, 0, 0, 20),
(2403, 'QUICK_ACTION', '查看报表', NULL, NULL, NULL, NULL, NULL, NULL, '/ledger/balance-sheet', 'TrendCharts', NULL, 'ENABLED', NULL, 1, 0, 0, 30),
(2404, 'QUICK_ACTION', '企业建模', NULL, NULL, NULL, NULL, NULL, NULL, '/enterprise-modeling', 'Link', NULL, 'ENABLED', NULL, 1, 0, 0, 40),
(2405, 'QUICK_ACTION', '我的档案', NULL, NULL, NULL, NULL, NULL, NULL, '/personal', 'User', NULL, 'ENABLED', NULL, 1, 0, 0, 50),
(2406, 'QUICK_ACTION', '日程日历', NULL, NULL, NULL, NULL, NULL, NULL, '/ledger/period-monitor-center', 'Calendar', NULL, 'ENABLED', NULL, 1, 0, 0, 60)
ON DUPLICATE KEY UPDATE
fsection = VALUES(fsection), fname = VALUES(fname), fdescription = VALUES(fdescription), fvalue = VALUES(fvalue),
fhint = VALUES(fhint), ftag = VALUES(ftag), fitem_type = VALUES(fitem_type), fpriority = VALUES(fpriority),
froute_path = VALUES(froute_path), ficon_key = VALUES(ficon_key), fstatus = VALUES(fstatus), fsort_no = VALUES(fsort_no);

INSERT INTO matrix_platform_module_item
(fid, fapp_code, fmodule_code, fsection, fname, fdescription, fvalue, fhint, fstatus, fstatus_text, froute_path, ficon_key, fprimary_flag, fsort_no)
VALUES
(3001, 'finance', 'ledger', 'STAT', '本月凭证', NULL, '1,286', '自动生成 64%', 'ENABLED', NULL, NULL, NULL, 0, 10),
(3002, 'finance', 'ledger', 'STAT', '待复核', NULL, '18', '协同检查 2 项', 'ENABLED', NULL, NULL, NULL, 0, 20),
(3003, 'finance', 'ledger', 'STAT', '期末任务', NULL, '9/12', '完成进度 75%', 'ENABLED', NULL, NULL, NULL, 0, 30),
(3004, 'finance', 'ledger', 'STAT', '现金流缺口', NULL, '4', '需补充项目', 'ENABLED', NULL, NULL, NULL, 0, 40),
(3011, 'finance', 'ledger', 'ACTION', '新增凭证', NULL, NULL, NULL, 'ENABLED', NULL, '/ledger/voucher', 'Tickets', 1, 10),
(3012, 'finance', 'ledger', 'ACTION', '月结工作台', NULL, NULL, NULL, 'ENABLED', NULL, '/ledger/month-end-close-workbench', 'Calendar', 0, 20),
(3021, 'finance', 'ledger', 'TOP_ACTION', '财务云', NULL, NULL, NULL, 'ENABLED', NULL, '/finance', 'Grid', 0, 10),
(3022, 'finance', 'ledger', 'TOP_ACTION', '月结', NULL, NULL, NULL, 'ENABLED', NULL, '/ledger/month-end-close-workbench', 'Calendar', 0, 20),
(3031, 'finance', 'ledger', 'FOCUS', '凭证协同检查', NULL, NULL, NULL, 'ENABLED', '待处理', '/ledger/voucher-collaboration-check', NULL, 0, 10),
(3032, 'finance', 'ledger', 'FOCUS', '现金流勾稽', NULL, NULL, NULL, 'ENABLED', '进行中', '/ledger/cashflow-notice-check', NULL, 0, 20),
(3033, 'finance', 'ledger', 'FOCUS', '往来核销', NULL, NULL, NULL, 'ENABLED', '可处理', '/ledger/counterparty-auto-writeoff', NULL, 0, 30),
(3041, 'finance', 'ledger', 'SHORTCUT', '凭证', NULL, NULL, NULL, 'ENABLED', NULL, '/ledger/voucher', 'Tickets', 0, 10),
(3042, 'finance', 'ledger', 'SHORTCUT', '总分类账', NULL, NULL, NULL, 'ENABLED', NULL, '/ledger/general-ledger', 'Collection', 0, 20),
(3043, 'finance', 'ledger', 'SHORTCUT', '科目余额表', NULL, NULL, NULL, 'ENABLED', NULL, '/ledger/subject-balance', 'List', 0, 30),
(3044, 'finance', 'ledger', 'SHORTCUT', '资产负债表', NULL, NULL, NULL, 'ENABLED', NULL, '/ledger/balance-sheet', 'DataAnalysis', 0, 40)
ON DUPLICATE KEY UPDATE
fsection = VALUES(fsection), fname = VALUES(fname), fdescription = VALUES(fdescription), fvalue = VALUES(fvalue),
fhint = VALUES(fhint), fstatus = VALUES(fstatus), fstatus_text = VALUES(fstatus_text), froute_path = VALUES(froute_path),
ficon_key = VALUES(ficon_key), fprimary_flag = VALUES(fprimary_flag), fsort_no = VALUES(fsort_no);

INSERT INTO matrix_platform_menu
(fid, fparent_id, fapp_code, fmodule_code, fmenu_code, fname, fdescription, fsummary, feyebrow, fmenu_type, froute_path, ficon_key, fstatus, fstatus_text, favailable, fsort_no)
VALUES
(4001, NULL, 'finance', 'ledger', 'ledger-voucher-group', '凭证处理', NULL, '录入、汇总、结转', 'VOUCHER', 'GROUP', NULL, 'Tickets', 'ENABLED', NULL, 1, 10),
(4002, 4001, 'finance', 'ledger', 'ledger-voucher', '凭证', '凭证录入与维护', NULL, NULL, 'PAGE', '/ledger/voucher', 'Files', 'ENABLED', '进入', 1, 11),
(4003, 4001, 'finance', 'ledger', 'ledger-voucher-summary', '凭证汇总表', '按期间查询凭证汇总', NULL, NULL, 'PAGE', '/ledger/voucher-summary', 'DataAnalysis', 'ENABLED', '进入', 1, 12),
(4004, 4001, 'finance', 'ledger', 'ledger-carry-list', '结转清单', '期末结转任务清单', NULL, NULL, 'PAGE', '/ledger/carry-list', 'Checked', 'ENABLED', '进入', 1, 13),
(4010, NULL, 'finance', 'ledger', 'ledger-book-query-group', '账表查询', NULL, '余额、总账、明细账', 'BOOK QUERY', 'GROUP', NULL, 'Collection', 'ENABLED', NULL, 1, 20),
(4011, 4010, 'finance', 'ledger', 'ledger-subject-balance', '科目余额表', '科目余额与发生额查询', NULL, NULL, 'PAGE', '/ledger/subject-balance', 'List', 'ENABLED', '进入', 1, 21),
(4012, 4010, 'finance', 'ledger', 'ledger-general-ledger', '总分类账', '科目总账查询', NULL, NULL, 'PAGE', '/ledger/general-ledger', 'Collection', 'ENABLED', '进入', 1, 22),
(4013, 4010, 'finance', 'ledger', 'ledger-detail-ledger', '明细分类账', '科目明细账查询', NULL, NULL, 'PAGE', '/ledger/detail-ledger', 'Files', 'ENABLED', '进入', 1, 23),
(4014, 4010, 'finance', 'ledger', 'ledger-daily-report', '日报表', '日维度账务报表', NULL, NULL, 'PAGE', '/ledger/daily-report', 'Calendar', 'ENABLED', '进入', 1, 24),
(4015, 4010, 'finance', 'ledger', 'ledger-dimension-balance', '核算维度余额表', '按核算维度查询余额', NULL, NULL, 'PAGE', '/ledger/dimension-balance', 'Grid', 'ENABLED', '进入', 1, 25),
(4016, 4010, 'finance', 'ledger', 'ledger-aux-dimension-balance', '辅助核算维度余额表', '辅助维度余额查询', NULL, NULL, 'PAGE', '/ledger/aux-dimension-balance', 'Grid', 'ENABLED', '进入', 1, 26),
(4017, 4010, 'finance', 'ledger', 'ledger-aux-general-ledger', '辅助总账', '辅助核算总账查询', NULL, NULL, 'PAGE', '/ledger/aux-general-ledger', 'Collection', 'ENABLED', '进入', 1, 27),
(4018, 4010, 'finance', 'ledger', 'ledger-aux-detail-ledger', '辅助明细账', '辅助核算明细查询', NULL, NULL, 'PAGE', '/ledger/aux-detail-ledger', 'Files', 'ENABLED', '进入', 1, 28),
(4020, NULL, 'finance', 'ledger', 'ledger-cash-flow-group', '现金流量', NULL, '报表、查询、补充资料', 'CASH FLOW', 'GROUP', NULL, 'TrendCharts', 'ENABLED', NULL, 1, 30),
(4021, 4020, 'finance', 'ledger', 'ledger-cash-flow', '现金流量表', '现金流入流出报表', NULL, NULL, 'PAGE', '/ledger/cash-flow', 'DataAnalysis', 'ENABLED', '进入', 1, 31),
(4022, 4020, 'finance', 'ledger', 'ledger-cash-flow-query', '现金流量查询', '现金流明细查询', NULL, NULL, 'PAGE', '/ledger/cash-flow-query', 'Search', 'ENABLED', '进入', 1, 32),
(4023, 4020, 'finance', 'ledger', 'ledger-cash-flow-supplement', '补充资料', '现金流量表补充资料', NULL, NULL, 'PAGE', '/ledger/cash-flow-supplement', 'Memo', 'ENABLED', '进入', 1, 33),
(4030, NULL, 'finance', 'ledger', 'ledger-counterparty-group', '往来管理', NULL, '核销、对账、账龄', 'COUNTERPARTY', 'GROUP', NULL, 'Wallet', 'ENABLED', NULL, 1, 40),
(4031, 4030, 'finance', 'ledger', 'ledger-counterparty-plan', '往来核销方案', '核销规则与方案配置', NULL, NULL, 'PAGE', '/ledger/counterparty-plan', 'SetUp', 'ENABLED', '进入', 1, 41),
(4032, 4030, 'finance', 'ledger', 'ledger-counterparty-auto-writeoff', '往来自动核销', '自动匹配与核销执行', NULL, NULL, 'PAGE', '/ledger/counterparty-auto-writeoff', 'Switch', 'ENABLED', '进入', 1, 42),
(4033, 4030, 'finance', 'ledger', 'ledger-counterparty-statement', '往来对账单', '客户与供应商对账单', NULL, NULL, 'PAGE', '/ledger/counterparty-statement', 'DocumentChecked', 'ENABLED', '进入', 1, 43),
(4034, 4030, 'finance', 'ledger', 'ledger-counterparty-account-query', '往来账查询', '往来账明细查询', NULL, NULL, 'PAGE', '/ledger/counterparty-account-query', 'Search', 'ENABLED', '进入', 1, 44),
(4035, 4030, 'finance', 'ledger', 'ledger-counterparty-writeoff-log', '往来核销日志', '核销执行记录', NULL, NULL, 'PAGE', '/ledger/counterparty-writeoff-log', 'List', 'ENABLED', '进入', 1, 45),
(4036, 4030, 'finance', 'ledger', 'ledger-counterparty-aging-analysis', '账龄分析表', '账龄结构分析', NULL, NULL, 'PAGE', '/ledger/counterparty-aging-analysis', 'TrendCharts', 'ENABLED', '进入', 1, 46),
(4037, 4030, 'finance', 'ledger', 'ledger-counterparty-multi-analysis', '往来多维分析表', '往来多维分析', NULL, NULL, 'PAGE', '/ledger/counterparty-multi-analysis', 'DataAnalysis', 'ENABLED', '进入', 1, 47),
(4040, NULL, 'finance', 'ledger', 'ledger-notice-group', '内部通知单', NULL, '往来与现金流通知', 'NOTICE', 'GROUP', NULL, 'DocumentChecked', 'ENABLED', NULL, 1, 50),
(4041, 4040, 'finance', 'ledger', 'ledger-counterparty-notice', '往来通知单', '往来业务通知单', NULL, NULL, 'PAGE', '/ledger/counterparty-notice', 'DocumentChecked', 'ENABLED', '进入', 1, 51),
(4042, 4040, 'finance', 'ledger', 'ledger-counterparty-notice-check', '往来通知单勾稽', '往来通知与凭证勾稽', NULL, NULL, 'PAGE', '/ledger/counterparty-notice-check', 'Checked', 'ENABLED', '进入', 1, 52),
(4043, 4040, 'finance', 'ledger', 'ledger-cashflow-notice', '现金流通知单', '现金流业务通知单', NULL, NULL, 'PAGE', '/ledger/cashflow-notice', 'Memo', 'ENABLED', '进入', 1, 53),
(4044, 4040, 'finance', 'ledger', 'ledger-cashflow-notice-check', '现金流通知单勾稽', '现金流通知与凭证勾稽', NULL, NULL, 'PAGE', '/ledger/cashflow-notice-check', 'Checked', 'ENABLED', '进入', 1, 54),
(4050, NULL, 'finance', 'ledger', 'ledger-collaboration-group', '账簿协同管理', NULL, '折算、对冲、检查', 'COLLABORATION', 'GROUP', NULL, 'Connection', 'ENABLED', NULL, 1, 60),
(4051, 4050, 'finance', 'ledger', 'ledger-voucher-rule', '凭证折算规则', '凭证折算与协同规划', NULL, NULL, 'PAGE', '/ledger/voucher-rule', 'SetUp', 'ENABLED', '进入', 1, 61),
(4052, 4050, 'finance', 'ledger', 'ledger-offset-voucher', '对冲凭证', '往来、现金流和调整凭证对冲', NULL, NULL, 'PAGE', '/ledger/offset-voucher', 'Switch', 'ENABLED', '进入', 1, 62),
(4053, 4050, 'finance', 'ledger', 'ledger-voucher-collaboration-check', '凭证协同检查', '自动凭证与人工复核', NULL, NULL, 'PAGE', '/ledger/voucher-collaboration-check', 'Connection', 'ENABLED', '进入', 1, 63),
(4054, 4050, 'finance', 'ledger', 'ledger-subject-compare', '科目余额对照', '科目余额对照检查', NULL, NULL, 'PAGE', '/ledger/subject-compare', 'DataAnalysis', 'ENABLED', '进入', 1, 64),
(4060, NULL, 'finance', 'ledger', 'ledger-period-group', '期末处理', NULL, '月结、转账、监控', 'PERIOD CLOSE', 'GROUP', NULL, 'Calendar', 'ENABLED', NULL, 1, 70),
(4061, 4060, 'finance', 'ledger', 'ledger-period-profit-loss', '结转损益', '期间损益结转', NULL, NULL, 'PAGE', '/ledger/period-profit-loss', 'Checked', 'ENABLED', '进入', 1, 71),
(4062, 4060, 'finance', 'ledger', 'ledger-period-auto-transfer', '自动转账', '自动转账规则执行', NULL, NULL, 'PAGE', '/ledger/period-auto-transfer', 'Switch', 'ENABLED', '进入', 1, 72),
(4063, 4060, 'finance', 'ledger', 'ledger-period-fx-revalue', '期末调汇', '外币期末重估', NULL, NULL, 'PAGE', '/ledger/period-fx-revalue', 'Money', 'ENABLED', '进入', 1, 73),
(4064, 4060, 'finance', 'ledger', 'ledger-period-voucher-amortization', '凭证摊销', '凭证摊销处理', NULL, NULL, 'PAGE', '/ledger/period-voucher-amortization', 'Files', 'ENABLED', '进入', 1, 74),
(4065, 4060, 'finance', 'ledger', 'ledger-period-close-books', '期末结账', '账簿封存与结账', NULL, NULL, 'PAGE', '/ledger/period-close-books', 'Checked', 'ENABLED', '进入', 1, 75),
(4066, 4060, 'finance', 'ledger', 'ledger-period-monitor-center', '监控中心', '期间处理状态监控', NULL, NULL, 'PAGE', '/ledger/period-monitor-center', 'TrendCharts', 'ENABLED', '进入', 1, 76),
(4067, 4060, 'finance', 'ledger', 'ledger-month-end-close-workbench', '月结工作台', '月结任务统一监控', NULL, NULL, 'PAGE', '/ledger/month-end-close-workbench', 'Calendar', 'ENABLED', '进入', 1, 77),
(4070, NULL, 'finance', 'ledger', 'ledger-report-group', '分析报表', NULL, '报表、指标、税表', 'REPORTS', 'GROUP', NULL, 'DataAnalysis', 'ENABLED', NULL, 1, 80),
(4071, 4070, 'finance', 'ledger', 'ledger-report-item', '报表项目', '报表项目维护', NULL, NULL, 'PAGE', '/ledger/report-item', 'List', 'ENABLED', '进入', 1, 81),
(4072, 4070, 'finance', 'ledger', 'ledger-financial-indicators', '财务指标', '经营指标分析', NULL, NULL, 'PAGE', '/ledger/financial-indicators', 'TrendCharts', 'ENABLED', '进入', 1, 82),
(4073, 4070, 'finance', 'ledger', 'ledger-balance-sheet', '资产负债表', '资产、负债与权益报表', NULL, NULL, 'PAGE', '/ledger/balance-sheet', 'DataAnalysis', 'ENABLED', '进入', 1, 83),
(4074, 4070, 'finance', 'ledger', 'ledger-profit-statement', '利润表', '收入、成本与利润分析', NULL, NULL, 'PAGE', '/ledger/profit-statement', 'TrendCharts', 'ENABLED', '进入', 1, 84),
(4075, 4070, 'finance', 'ledger', 'ledger-enterprise-tax', '企业纳税表', '税务口径报表', NULL, NULL, 'PAGE', '/ledger/enterprise-tax', 'DocumentChecked', 'ENABLED', '进入', 1, 85),
(4076, 4070, 'finance', 'ledger', 'ledger-cash-flow-report', '现金流量表', '现金流入流出报表', NULL, NULL, 'PAGE', '/ledger/cash-flow', 'DataAnalysis', 'ENABLED', '进入', 1, 86),
(4080, NULL, 'finance', 'ledger', 'ledger-opening-group', '初始化', NULL, '期初余额与现金流', 'INITIALIZATION', 'GROUP', NULL, 'SetUp', 'ENABLED', NULL, 1, 90),
(4081, 4080, 'finance', 'ledger', 'ledger-opening-subject', '科目余额初始化', '科目期初余额', NULL, NULL, 'PAGE', '/ledger/opening-subject', 'Files', 'ENABLED', '进入', 1, 91),
(4082, 4080, 'finance', 'ledger', 'ledger-opening-cashflow', '现金流初始化', '现金流期初数据', NULL, NULL, 'PAGE', '/ledger/opening-cashflow', 'TrendCharts', 'ENABLED', '进入', 1, 92),
(4083, 4080, 'finance', 'ledger', 'ledger-opening-counterparty', '往来余额初始化', '往来期初余额', NULL, NULL, 'PAGE', '/ledger/opening-counterparty', 'Wallet', 'ENABLED', '进入', 1, 93),
(4090, NULL, 'finance', 'ledger', 'ledger-config-group', '基础设置', NULL, '类型、维度、映射', 'CONFIGURATION', 'GROUP', NULL, 'SetUp', 'ENABLED', NULL, 1, 100),
(4091, 4090, 'finance', 'ledger', 'ledger-voucher-type', '凭证类型', '凭证字与类型设置', NULL, NULL, 'PAGE', '/ledger/voucher-type', 'Tickets', 'ENABLED', '进入', 1, 101),
(4092, 4090, 'finance', 'ledger', 'ledger-cashflow-item', '现金流量项目', '现金流项目配置', NULL, NULL, 'PAGE', '/ledger/cashflow-item', 'TrendCharts', 'ENABLED', '进入', 1, 102),
(4093, 4090, 'finance', 'ledger', 'ledger-report-account-map', '报表科目映射', '报表项目与科目关系', NULL, NULL, 'PAGE', '/ledger/report-account-map', 'Connection', 'ENABLED', '进入', 1, 103),
(4094, 4090, 'finance', 'ledger', 'ledger-base-config-dimension-relation', '核算维度关系设置', '核算维度关联规则', NULL, NULL, 'PAGE', '/ledger/base-config-dimension-relation', 'Grid', 'ENABLED', '进入', 1, 104),
(4095, 4090, 'finance', 'ledger', 'ledger-base-config-dimension-value-range', '核算维度值范围设置', '维度值范围控制', NULL, NULL, 'PAGE', '/ledger/base-config-dimension-value-range', 'SetUp', 'ENABLED', '进入', 1, 105),
(4096, 4090, 'finance', 'ledger', 'ledger-base-config-equity-change-type', '所有者权益变动类型', '权益变动类型配置', NULL, NULL, 'PAGE', '/ledger/base-config-equity-change-type', 'DataAnalysis', 'ENABLED', '进入', 1, 106),
(4097, 4090, 'finance', 'ledger', 'ledger-base-config-impairment-nature', '减值准备性质', '减值准备性质配置', NULL, NULL, 'PAGE', '/ledger/base-config-impairment-nature', 'DataAnalysis', 'ENABLED', '进入', 1, 107),
(4098, 4090, 'finance', 'ledger', 'ledger-base-config-license-plate-item', '车辆牌照号项目', '车辆牌照项目配置', NULL, NULL, 'PAGE', '/ledger/base-config-license-plate-item', 'List', 'ENABLED', '进入', 1, 108),
(4099, 4090, 'finance', 'ledger', 'ledger-base-config-cost-nature', '成本性质', '成本性质配置', NULL, NULL, 'PAGE', '/ledger/base-config-cost-nature', 'Money', 'ENABLED', '进入', 1, 109)
ON DUPLICATE KEY UPDATE
fparent_id = VALUES(fparent_id), fmodule_code = VALUES(fmodule_code), fname = VALUES(fname),
fdescription = VALUES(fdescription), fsummary = VALUES(fsummary), feyebrow = VALUES(feyebrow),
fmenu_type = VALUES(fmenu_type), froute_path = VALUES(froute_path), ficon_key = VALUES(ficon_key),
fstatus = VALUES(fstatus), fstatus_text = VALUES(fstatus_text), favailable = VALUES(favailable), fsort_no = VALUES(fsort_no);
