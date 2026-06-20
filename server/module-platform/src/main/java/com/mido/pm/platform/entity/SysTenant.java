package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 租户注册表（sys_tenant）。平台域全局表，不带 tenant_id；本表的 id 即业务侧各表的 tenant_id。
 * status：trial 试用 / active 正式 / suspended 停用 / expired 已过期 / closed 已注销。
 */
@TableName("sys_tenant")
public class SysTenant extends PlatformBaseEntity {

    /** 租户编码（程序引用，租户内全局唯一，可作子域名） */
    private String code;
    private String name;
    /** trial/active/suspended/expired/closed */
    private String status;
    private String industry;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    /** 来源渠道：manual 运营手动 / 其他预留 */
    private String source;
    /** 租户主管理员对应的 sys_user.id（运营开通时初始化，可空） */
    private Long adminUserId;
    private LocalDateTime activatedAt;
    /** 到期时间（与订阅同步，空表示不限期，如自用租户） */
    private LocalDateTime expireAt;
    /** 注销宽限期满后的计划清除时间（注销合规）；空表示未进入注销流程 */
    private LocalDateTime purgeScheduledAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public LocalDateTime getPurgeScheduledAt() {
        return purgeScheduledAt;
    }

    public void setPurgeScheduledAt(LocalDateTime purgeScheduledAt) {
        this.purgeScheduledAt = purgeScheduledAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
