package single.cjj.bizfi.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 业务单元实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bizfi_base_org")
public class BizfiBizUnit implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId("fid")
    private Long fid;

    /** 编码 */
    private String fcode;

    /** 名称 */
    private String fname;

    /** 简称 */
    private String fshortName;

    /** 形态 */
    private String fform;

    /** 形态类型 */
    private String fformType;

    /** 上级业务单元ID */
    private Long fparentOrgId;

    /** 描述 */
    private String fdescription;

    /** 来源系统 */
    private String fsourceSystem;

    /** 管理组织编码 */
    private String fmanageOrgCode;

    /** 管理组织名称 */
    private String fmanageOrgName;

    /** 上级管理组织编码 */
    private String fparentManageOrgCode;

    /** 投资方式 */
    private String finvestmentType;

    /** 所属行业代码 */
    private String findustryCode;

    /** 所属行业名称 */
    private String findustryName;

    /** 持股比例% */
    private BigDecimal fholdingRatio;

    /** 大区 */
    private String fregionArea;

    /** 区域 */
    private String farea;

    /** 默认法人组织 */
    private String fdefaultLegalEntity;

    /** 记账组织 */
    private String faccountingOrg;

    /** 管理组织类型 */
    private String fmanageOrgType;

    /** 总账 */
    private String fglCode;

    /** 币种 */
    private String fcurrency;

    /** 注册资本（万元） */
    private BigDecimal fregistCapital;

    /** 是否上市公司 */
    private Boolean fisListedCompany;

    /** 退出日期 */
    private LocalDate fexitDate;

    /** 法人组织状态 */
    private String flegalEntityStatus;

    /** 是否并表 */
    private Boolean fisConsolidated;

    /** 实际控制形式 */
    private String factualControlType;

    /** 集团分红比例（%） */
    private BigDecimal fgroupDividendRatio;

    /** 上级组织 */
    private String fparentOrgCode;

    /** 收入成本类型 */
    private String fincomeCostType;

    /** 是否走审批 */
    private Boolean fisNeedApproval;

    /** 是否分公司 */
    private Boolean fisBranchCompany;

    /** 是否走法人审核 */
    private Boolean fisLegalEntityApproval;

    /** 法人公司分类 */
    private String flegalEntityCategory;

    /** 组织结构 */
    private String forgStructure;

    /** 使用状态 */
    private String fusagestatus;

    /** 数据状态 */
    private String fdataStatus;

    /** 职能类型 */
    private String ffunctionType;

    /** 国家/地区 */
    private String fcountry;

    /** 城市 */
    private String fcity;

    /** 时区 */
    private String ftimezone;

    /** 邮编 */
    private String fzipcode;

    /** 联系电话 */
    private String fcontactPhone;

    /** 联系地址 */
    private String fcontactAddress;

    /** 企业工商税务详情 */
    private String fenterpriseTaxDetail;

    /** 统一社会信用代码 */
    private String funifiedSocialCreditCode;

    /** 公司名称 */
    private String fcompanyName;

    /** 公司类型 */
    private String fcompanyType;

    /** 法定代表人 */
    private String flegalRepresentative;

    /** 住所 */
    private String fdomicile;

    /** 注册资本 */
    private String fregistCapitalDetail;

    /** 成立日期 */
    private LocalDate festablishmentDate;

    /** 营业期限 */
    private String fbusinessTerm;

    /** 经营范围 */
    private String fbusinessScope;

    /** 纳税识别号 */
    private String ftaxpayerId;

    /** 公司电话 */
    private String fcompanyPhone;

    /** 开户行 */
    private String fbankName;

    /** 银行账户 */
    private String fbankAccount;

    /** 纳税人类型 */
    private String ftaxpayerType;

    /** 计税方式 */
    private String ftaxMethod;

    /** 创建时间 */
    private LocalDateTime fcreatedAt;

    /** 修改时间 */
    private LocalDateTime fupdatedAt;

    /** 创建人 */
    private String fcreatedBy;

    /** 修改人 */
    private String fupdatedBy;
}
