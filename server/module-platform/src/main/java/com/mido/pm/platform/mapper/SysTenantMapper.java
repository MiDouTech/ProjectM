package com.mido.pm.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.platform.entity.SysTenant;
import org.apache.ibatis.annotations.Mapper;

/** SysTenant Mapper（平台域全局表，不参与多租户隔离）。 */
@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {
}
