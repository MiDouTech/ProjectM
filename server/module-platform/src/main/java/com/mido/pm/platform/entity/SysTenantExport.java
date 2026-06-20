package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 租户数据导出任务（sys_tenant_export）。平台域全局表，tenant_id 为引用列。
 * status：pending 待处理 / processing 处理中 / done 完成 / failed 失败。
 * 异步：创建为 pending，定时处理器聚合各域数据→JSON→对象存储，写 file_key。
 */
@TableName("sys_tenant_export")
public class SysTenantExport extends PlatformBaseEntity {

    private Long tenantId;
    private String status;
    /** 对象存储 key（完成后填，下载走预签名 URL，不外泄） */
    private String fileKey;
    private String error;
    /** 发起的平台运营账号 ID */
    private Long requestedBy;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Long getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(Long requestedBy) {
        this.requestedBy = requestedBy;
    }
}
