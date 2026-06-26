package com.mido.pm.report.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.report.dto.ReportSettingVO;
import com.mido.pm.report.entity.PmReportSetting;
import com.mido.pm.report.mapper.PmReportSettingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报表设置（租户级）：财年起始月。每租户一行，缺省视为 1（自然年）。
 * tenant_id 由多租户拦截器注入，业务不手写条件。
 */
@Service
public class ReportSettingService {

    /** 缺省财年起始月 = 1（自然年）。 */
    public static final int DEFAULT_FISCAL_START_MONTH = 1;

    private final PmReportSettingMapper mapper;

    public ReportSettingService(PmReportSettingMapper mapper) {
        this.mapper = mapper;
    }

    public ReportSettingVO get() {
        return new ReportSettingVO(fiscalYearStartMonth());
    }

    /** 当前租户财年起始月（1-12）；未配置返回缺省 1。 */
    public int fiscalYearStartMonth() {
        PmReportSetting s = current();
        return s == null || s.getFiscalYearStartMonth() == null
                ? DEFAULT_FISCAL_START_MONTH : s.getFiscalYearStartMonth();
    }

    /** 设置财年起始月（1-12），upsert。 */
    @Transactional(rollbackFor = Exception.class)
    public void setFiscalYearStartMonth(int month) {
        if (month < 1 || month > 12) {
            throw new BizException(ErrorCode.PARAM_ERROR, "财年起始月须在 1-12 之间");
        }
        PmReportSetting s = current();
        if (s == null) {
            s = new PmReportSetting();
            s.setFiscalYearStartMonth(month);
            mapper.insert(s);
        } else {
            s.setFiscalYearStartMonth(month);
            mapper.updateById(s);
        }
    }

    private PmReportSetting current() {
        return mapper.selectOne(Wrappers.<PmReportSetting>lambdaQuery().last("limit 1"));
    }
}
