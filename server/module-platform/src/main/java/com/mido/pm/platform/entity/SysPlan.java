package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/**
 * 套餐（sys_plan）。平台域全局表。price 为线下收款参考价（阶段一不接在线支付）。
 * billingCycle：monthly/yearly/once；status：active/disabled。
 */
@TableName("sys_plan")
public class SysPlan extends PlatformBaseEntity {

    private String code;
    private String name;
    /** 线下参考价（不接支付网关） */
    private BigDecimal price;
    /** monthly/yearly/once */
    private String billingCycle;
    /** active/disabled */
    private String status;
    private Integer sort;
    private String remark;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
