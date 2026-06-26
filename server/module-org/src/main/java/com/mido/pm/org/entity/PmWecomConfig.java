package com.mido.pm.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 企业微信集成配置（pm_wecom_config）。租户业务表，单租户一行。
 * 三类 secret 列存 AES 密文（{@link com.mido.pm.common.security.SecretCipher}），明文不入库、不出接口。
 */
@TableName("pm_wecom_config")
public class PmWecomConfig extends BaseEntity {

    /** 企业 ID（CorpID），三类能力共用 */
    private String corpId;

    /** 通讯录同步开关 0/1 */
    private Integer contactsEnabled;
    /** 通讯录 Secret（AES 密文） */
    private String contactsSecret;

    /** SSO 登录开关 0/1 */
    private Integer ssoEnabled;
    private String ssoAgentId;
    /** SSO Secret（AES 密文） */
    private String ssoSecret;

    /** 消息推送开关 0/1 */
    private Integer msgEnabled;
    private String msgAgentId;
    /** 消息 Secret（AES 密文） */
    private String msgSecret;

    private LocalDateTime lastSyncAt;
    private String lastSyncResult;

    public String getCorpId() { return corpId; }
    public void setCorpId(String corpId) { this.corpId = corpId; }

    public Integer getContactsEnabled() { return contactsEnabled; }
    public void setContactsEnabled(Integer contactsEnabled) { this.contactsEnabled = contactsEnabled; }

    public String getContactsSecret() { return contactsSecret; }
    public void setContactsSecret(String contactsSecret) { this.contactsSecret = contactsSecret; }

    public Integer getSsoEnabled() { return ssoEnabled; }
    public void setSsoEnabled(Integer ssoEnabled) { this.ssoEnabled = ssoEnabled; }

    public String getSsoAgentId() { return ssoAgentId; }
    public void setSsoAgentId(String ssoAgentId) { this.ssoAgentId = ssoAgentId; }

    public String getSsoSecret() { return ssoSecret; }
    public void setSsoSecret(String ssoSecret) { this.ssoSecret = ssoSecret; }

    public Integer getMsgEnabled() { return msgEnabled; }
    public void setMsgEnabled(Integer msgEnabled) { this.msgEnabled = msgEnabled; }

    public String getMsgAgentId() { return msgAgentId; }
    public void setMsgAgentId(String msgAgentId) { this.msgAgentId = msgAgentId; }

    public String getMsgSecret() { return msgSecret; }
    public void setMsgSecret(String msgSecret) { this.msgSecret = msgSecret; }

    public LocalDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(LocalDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    public String getLastSyncResult() { return lastSyncResult; }
    public void setLastSyncResult(String lastSyncResult) { this.lastSyncResult = lastSyncResult; }
}
