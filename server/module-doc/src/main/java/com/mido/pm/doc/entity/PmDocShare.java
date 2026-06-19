package com.mido.pm.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/** 文档公开分享（pm_doc_share）：token 免登录只读访问。 */
@TableName("pm_doc_share")
public class PmDocShare extends BaseEntity {
    private Long docId;
    private String token;
    private LocalDateTime expireTime;
    private Integer enabled;

    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
