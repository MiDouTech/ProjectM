package com.mido.pm.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.task.entity.PmStatus;
import org.apache.ibatis.annotations.Mapper;

/** 状态库 Mapper。 */
@Mapper
public interface PmStatusMapper extends BaseMapper<PmStatus> {
}
