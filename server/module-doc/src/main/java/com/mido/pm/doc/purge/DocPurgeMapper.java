package com.mido.pm.doc.purge;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 文档/附件域物理清除（注销合规）。子表先于主表；附件需先删对象存储文件。 */
@Mapper
public interface DocPurgeMapper {

    /** 取该租户全部附件对象存储 key（删行前用于清理对象存储文件）。 */
    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT oss_key FROM pm_attachment WHERE tenant_id = #{t} AND oss_key IS NOT NULL")
    List<String> selectOssKeys(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_doc_favorite WHERE tenant_id = #{t}")
    int purgeFavorites(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_doc_share WHERE tenant_id = #{t}")
    int purgeShares(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_doc_acl WHERE tenant_id = #{t}")
    int purgeAcls(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_doc_version WHERE tenant_id = #{t}")
    int purgeVersions(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_doc WHERE tenant_id = #{t}")
    int purgeDocs(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_attachment WHERE tenant_id = #{t}")
    int purgeAttachments(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_doc_template WHERE tenant_id = #{t}")
    int purgeTemplates(@Param("t") Long tenantId);
}
