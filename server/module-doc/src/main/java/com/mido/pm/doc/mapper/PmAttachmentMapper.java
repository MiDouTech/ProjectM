package com.mido.pm.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.doc.entity.PmAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 附件 Mapper。 */
@Mapper
public interface PmAttachmentMapper extends BaseMapper<PmAttachment> {

    /**
     * 当前租户附件总字节数（用量统计存储维度）。
     * 多租户拦截器会自动为本 SQL 注入 tenant_id 条件，故无需手写租户过滤。
     */
    @Select("SELECT COALESCE(SUM(size), 0) FROM pm_attachment WHERE is_deleted = 0")
    long sumSizeBytes();
}
