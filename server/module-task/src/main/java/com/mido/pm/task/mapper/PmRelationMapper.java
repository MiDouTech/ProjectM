package com.mido.pm.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.task.entity.PmRelation;
import org.apache.ibatis.annotations.Mapper;

/** 工作项关联(实例) Mapper。 */
@Mapper
public interface PmRelationMapper extends BaseMapper<PmRelation> {
}
