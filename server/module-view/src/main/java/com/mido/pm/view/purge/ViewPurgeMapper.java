package com.mido.pm.view.purge;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 视图/页面/导航配置域物理清除（注销合规）。 */
@Mapper
public interface ViewPurgeMapper {

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_page_config WHERE tenant_id = #{t}")
    int purgePageConfigs(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_view WHERE tenant_id = #{t}")
    int purgeViews(@Param("t") Long tenantId);

    @InterceptorIgnore(tenantLine = "1")
    @Delete("DELETE FROM pm_module_nav WHERE tenant_id = #{t}")
    int purgeModuleNavs(@Param("t") Long tenantId);
}
