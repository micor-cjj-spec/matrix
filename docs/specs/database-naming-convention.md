# Matrix 数据库命名规范

## 1. 文档目标

本文档定义 Matrix 项目的数据库命名强制规范。

以后每次设计数据库、表、字段、SQL 交付物、后端实体映射、数据字典时，都必须先遵守本文档规则。

## 2. 适用范围

本规范适用于：

- 数据库名
- 表名
- 字段名
- 主键字段
- 外键或引用字段
- 通用审计字段
- docs、prompt、deliverables、代码注释中的 SQL 示例

历史遗留对象可以等专项迁移时再统一调整。新的数据库设计不得继续引入不符合本规范的命名。

## 3. 强制命名规则

### 3.1 数据库名

数据库名必须使用：

```text
matrix_{模块名}
```

规则：

- 固定以 `matrix_` 开头。
- `{模块名}` 使用小写英文和下划线。
- 模块名应使用稳定、简短、能表达业务边界的编码。
- 不能使用临时项目名、服务名、页面名或产品别名作为数据库名。

示例：

```text
matrix_fi
matrix_base
matrix_auth
matrix_platform
matrix_supply
matrix_shared
```

### 3.2 表名

表名必须使用：

```text
matrix_{模块名}_{业务表名}
```

规则：

- 表名前缀必须与数据库模块名一致。
- `{业务表名}` 根据业务对象命名，使用小写英文和下划线。
- 表名按业务取，不按页面取。
- 如果业务对象属于稳定子模块，可以把子模块编码放进业务表名。
- 避免使用 `data`、`info`、`list`、`temp` 这类含义模糊的后缀，除非它本身就是明确业务概念。

示例：

```text
matrix_fi_voucher
matrix_fi_voucher_entry
matrix_fi_gl_account_balance
matrix_fi_ap_payable_doc
matrix_fi_ar_receivable_doc
matrix_base_customer
matrix_base_supplier
matrix_platform_app
matrix_platform_menu
matrix_platform_role_menu
matrix_platform_user_shortcut
```

### 3.3 字段名

字段名必须以 `f` 开头：

```text
f{业务字段名}
```

规则：

- 每个字段都必须以小写 `f` 开头。
- 多单词字段使用小写英文和下划线。
- 不能混用 `id`、`name`、`status`、`created_at` 这类无 `f` 前缀字段。
- 主键统一为 `fid`。
- 名称字段优先使用 `fname`。
- 编码字段优先使用 `fcode`。
- 状态字段优先使用 `fstatus`。

示例：

```text
fid
fname
fcode
fstatus
fapp_code
fmenu_code
froute_path
fparent_id
fuser_id
frole_id
fcreate_time
fmodify_time
```

### 3.4 主键与引用字段

主键字段固定为：

```text
fid
```

外键或引用字段使用：

```text
f{引用业务对象}_id
```

示例：

```text
fparent_id
fuser_id
frole_id
fmenu_id
forg_id
fvoucher_id
fcustomer_id
fsupplier_id
```

### 3.5 通用系统字段

表需要生命周期、审计、租户、组织隔离能力时，统一使用：

```text
fcreate_by
fcreate_time
fmodify_by
fmodify_time
fdelete_flag
fversion
ftenant_id
forg_id
```

规则：

- 逻辑删除字段使用 `fdelete_flag`。
- 乐观锁字段使用 `fversion`。
- 多租户隔离字段使用 `ftenant_id`。
- 组织隔离字段使用 `forg_id`。

## 4. SQL 设计检查清单

提交任何新的数据库设计前，必须检查：

- 数据库名是否符合 `matrix_{模块名}`。
- 表名是否符合 `matrix_{模块名}_{业务表名}`。
- 每个字段是否都以 `f` 开头。
- 主键是否为 `fid`。
- 引用字段是否使用 `fxxx_id`。
- 数据库模块名与表名前缀是否一致。
- docs、prompt、deliverables 中的 SQL 示例是否同样遵守本规范。

## 5. 示例

平台工作台菜单配置：

```sql
CREATE DATABASE IF NOT EXISTS matrix_platform;

CREATE TABLE matrix_platform_menu (
    fid BIGINT NOT NULL COMMENT '主键',
    fparent_id BIGINT NULL COMMENT '父级菜单ID',
    fapp_code VARCHAR(64) NOT NULL COMMENT '应用编码',
    fmenu_code VARCHAR(64) NOT NULL COMMENT '菜单编码',
    fname VARCHAR(128) NOT NULL COMMENT '菜单名称',
    fmenu_type VARCHAR(32) NOT NULL COMMENT '菜单类型',
    froute_path VARCHAR(255) NULL COMMENT '前端路由路径',
    ficon_key VARCHAR(64) NULL COMMENT '前端图标编码',
    fsort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    fstatus VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    fcreate_time DATETIME NOT NULL COMMENT '创建时间',
    fmodify_time DATETIME NULL COMMENT '修改时间',
    PRIMARY KEY (fid),
    KEY idx_matrix_platform_menu_app_parent (fapp_code, fparent_id),
    KEY idx_matrix_platform_menu_code (fmenu_code)
) COMMENT='Matrix平台菜单配置';
```

财务凭证表：

```sql
CREATE DATABASE IF NOT EXISTS matrix_fi;

CREATE TABLE matrix_fi_voucher (
    fid BIGINT NOT NULL COMMENT '主键',
    fvoucher_no VARCHAR(64) NOT NULL COMMENT '凭证号',
    fvoucher_date DATE NOT NULL COMMENT '凭证日期',
    fperiod VARCHAR(16) NOT NULL COMMENT '会计期间',
    fstatus VARCHAR(32) NOT NULL COMMENT '凭证状态',
    fcreate_by BIGINT NULL COMMENT '创建人ID',
    fcreate_time DATETIME NOT NULL COMMENT '创建时间',
    fmodify_by BIGINT NULL COMMENT '修改人ID',
    fmodify_time DATETIME NULL COMMENT '修改时间',
    PRIMARY KEY (fid),
    KEY idx_matrix_fi_voucher_period (fperiod),
    KEY idx_matrix_fi_voucher_no (fvoucher_no)
) COMMENT='Matrix财务凭证';
```

## 6. 后续工作要求

以后设计任何 Matrix 数据库对象时，必须先从本文档开始。

如果提出的库名、表名或字段名不符合本规范，必须先修正命名，再继续表结构设计、后端实体设计、前端接口设计或 SQL 交付物编写。
