package com.mido.pm.cost.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费用（pm_cost）。account 科目（住宿/餐费/差旅/制作/服务费...）；
 * status 未发生/已发生/被退回；approval_id 关联审批实例（biz_type=cost）。公共字段见 {@link BaseEntity}。
 */
@TableName("pm_cost")
public class PmCost extends BaseEntity {

    private Long projectId;
    private String title;
    private String account;
    private BigDecimal budgetAmount;
    private BigDecimal actualAmount;
    private LocalDate occurDate;
    private LocalDate payDate;
    private String status;
    private Long approvalId;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
    public LocalDate getOccurDate() { return occurDate; }
    public void setOccurDate(LocalDate occurDate) { this.occurDate = occurDate; }
    public LocalDate getPayDate() { return payDate; }
    public void setPayDate(LocalDate payDate) { this.payDate = payDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getApprovalId() { return approvalId; }
    public void setApprovalId(Long approvalId) { this.approvalId = approvalId; }
}
