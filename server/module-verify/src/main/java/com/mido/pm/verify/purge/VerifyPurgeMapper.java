package com.mido.pm.verify.purge;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 验收(NPSS)域物理清除（注销合规）。子表先于主表。 */
@Mapper
public interface VerifyPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_npss_score WHERE tenant_id = #{t}")
    int purgeScores(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_npss_subject_member WHERE tenant_id = #{t}")
    int purgeSubjectMembers(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_npss_review WHERE tenant_id = #{t}")
    int purgeReviews(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_npss_subject WHERE tenant_id = #{t}")
    int purgeSubjects(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_npss_subject_template WHERE tenant_id = #{t}")
    int purgeSubjectTemplates(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_result_verify WHERE tenant_id = #{t}")
    int purgeResultVerifies(@Param("t") Long tenantId);
}
