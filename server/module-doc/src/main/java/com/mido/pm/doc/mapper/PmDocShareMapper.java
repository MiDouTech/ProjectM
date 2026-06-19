package com.mido.pm.doc.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.doc.entity.PmDocShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 文档分享 Mapper。 */
@Mapper
public interface PmDocShareMapper extends BaseMapper<PmDocShare> {

    /**
     * 按 token 全局查询（跳过多租户过滤）：公开外链匿名访问无租户上下文，token 全局唯一，
     * 命中后再由调用方按 share.tenant_id 设置上下文加载文档。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM pm_doc_share WHERE token = #{token} AND is_deleted = 0 LIMIT 1")
    PmDocShare selectByTokenGlobal(String token);
}
