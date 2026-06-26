package com.mido.pm.verify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结果验收（铁三角）结论（pm_result_verify）。每次验收一条，最新一条为权威。
 * verdict：pass 达标 / fail 不达标。onTime/inBudget/inScope 为时间-成本-范围三项达标快照，
 * 自动指标由前端按项目数据与完成率预填、PMO 可调整后录入；verdict=pass 方可结案。
 */
@TableName("pm_result_verify")
public class PmResultVerify extends BaseEntity {

    private Long projectId;
    private String verdict;
    private Integer onTime;
    private Integer inBudget;
    private Integer inScope;
    private BigDecimal completionRate;
    private String remark;
    private Long verifiedBy;
    private LocalDateTime verifiedAt;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public Integer getOnTime() { return onTime; }
    public void setOnTime(Integer onTime) { this.onTime = onTime; }
    public Integer getInBudget() { return inBudget; }
    public void setInBudget(Integer inBudget) { this.inBudget = inBudget; }
    public Integer getInScope() { return inScope; }
    public void setInScope(Integer inScope) { this.inScope = inScope; }
    public BigDecimal getCompletionRate() { return completionRate; }
    public void setCompletionRate(BigDecimal completionRate) { this.completionRate = completionRate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(Long verifiedBy) { this.verifiedBy = verifiedBy; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
