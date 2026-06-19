package com.mido.pm.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/** 文档收藏（pm_doc_favorite）：user_id + doc_id。 */
@TableName("pm_doc_favorite")
public class PmDocFavorite extends BaseEntity {
    private Long userId;
    private Long docId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }
}
