package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 线下收入台账（sys_revenue_record）。平台域全局表，tenant_id 为普通引用列。
 * 记录线下收款/退款/合同流水（阶段一不接在线支付）。type：payment 收款 / refund 退款。
 */
@TableName("sys_revenue_record")
public class SysRevenueRecord extends PlatformBaseEntity {

    private Long tenantId;
    /** payment 收款 / refund 退款 */
    private String type;
    private BigDecimal amount;
    private String contractNo;
    private LocalDate occurredDate;
    private String remark;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public LocalDate getOccurredDate() {
        return occurredDate;
    }

    public void setOccurredDate(LocalDate occurredDate) {
        this.occurredDate = occurredDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
