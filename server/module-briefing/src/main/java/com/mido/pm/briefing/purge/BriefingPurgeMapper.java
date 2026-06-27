package com.mido.pm.briefing.purge;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 简报域物理清除（注销合规）。子表先于主表。 */
@Mapper
public interface BriefingPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_briefing_recipient WHERE tenant_id = #{t}")
    int purgeRecipients(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_briefing_review WHERE tenant_id = #{t}")
    int purgeReviews(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_briefing_issue WHERE tenant_id = #{t}")
    int purgeIssues(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_briefing_assignment WHERE tenant_id = #{t}")
    int purgeAssignments(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_briefing WHERE tenant_id = #{t}")
    int purgeBriefings(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_briefing_template WHERE tenant_id = #{t}")
    int purgeTemplates(@Param("t") Long tenantId);
}
