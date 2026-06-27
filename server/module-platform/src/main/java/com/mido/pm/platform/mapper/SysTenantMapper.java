package com.mido.pm.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.platform.entity.SysTenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** SysTenant Mapper（平台域全局表，不参与多租户隔离）。 */
@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {

    /** 按月统计新增租户数（用于增长趋势）。返回 [{ym:'YYYY-MM', cnt:n}]。 */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS ym, COUNT(*) AS cnt "
            + "FROM sys_tenant WHERE is_deleted = 0 AND create_time >= #{since} "
            + "GROUP BY ym ORDER BY ym")
    List<Map<String, Object>> monthlyRegistrations(@Param("since") LocalDateTime since);
}
