package com.mido.pm.collab.purge;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 协作(评论/通知)域物理清除（注销合规）。 */
@Mapper
public interface CollabPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_notification WHERE tenant_id = #{t}")
    int purgeNotifications(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_comment WHERE tenant_id = #{t}")
    int purgeComments(@Param("t") Long tenantId);
}
