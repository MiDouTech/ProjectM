package com.mido.pm.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.org.entity.PmWecomConfig;
import org.apache.ibatis.annotations.Mapper;

/** 企业微信集成配置 Mapper。租户隔离由多租户拦截器统一注入。 */
@Mapper
public interface PmWecomConfigMapper extends BaseMapper<PmWecomConfig> {
}
