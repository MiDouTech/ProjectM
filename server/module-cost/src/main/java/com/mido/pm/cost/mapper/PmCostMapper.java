package com.mido.pm.cost.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.cost.entity.PmCost;
import org.apache.ibatis.annotations.Mapper;

/** 费用 Mapper。 */
@Mapper
public interface PmCostMapper extends BaseMapper<PmCost> {
}
