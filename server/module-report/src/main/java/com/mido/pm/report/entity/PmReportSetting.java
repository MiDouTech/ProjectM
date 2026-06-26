package com.mido.pm.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 报表设置（pm_report_setting，租户级）。fiscalYearStartMonth=1 即自然年（默认），=4 即 4 月起财年。
 */
@TableName("pm_report_setting")
public class PmReportSetting extends BaseEntity {

    private Integer fiscalYearStartMonth;

    public Integer getFiscalYearStartMonth() { return fiscalYearStartMonth; }
    public void setFiscalYearStartMonth(Integer fiscalYearStartMonth) { this.fiscalYearStartMonth = fiscalYearStartMonth; }
}
