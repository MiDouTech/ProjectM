package com.mido.pm.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.report.entity.PmReportSetting;
import org.apache.ibatis.annotations.Mapper;

/** 报表设置 Mapper（租户级）。 */
@Mapper
public interface PmReportSettingMapper extends BaseMapper<PmReportSetting> {
}
