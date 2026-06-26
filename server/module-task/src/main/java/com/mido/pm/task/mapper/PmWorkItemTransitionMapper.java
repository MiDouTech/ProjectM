package com.mido.pm.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.task.entity.PmWorkItemTransition;
import org.apache.ibatis.annotations.Mapper;

/** 工作项类型状态转移 Mapper。 */
@Mapper
public interface PmWorkItemTransitionMapper extends BaseMapper<PmWorkItemTransition> {
}
