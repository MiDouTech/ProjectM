package com.mido.pm.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.task.entity.PmWorkflowTransition;
import org.apache.ibatis.annotations.Mapper;

/** 工作流状态转移 Mapper。 */
@Mapper
public interface PmWorkflowTransitionMapper extends BaseMapper<PmWorkflowTransition> {
}
