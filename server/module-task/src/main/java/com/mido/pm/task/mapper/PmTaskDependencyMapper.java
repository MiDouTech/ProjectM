package com.mido.pm.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.task.entity.PmTaskDependency;
import org.apache.ibatis.annotations.Mapper;

/** 任务依赖 Mapper。 */
@Mapper
public interface PmTaskDependencyMapper extends BaseMapper<PmTaskDependency> {
}
