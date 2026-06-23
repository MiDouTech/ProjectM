package com.mido.pm.briefing.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.briefing.dto.BriefingTemplateVO;
import com.mido.pm.briefing.dto.FieldDefVO;
import com.mido.pm.briefing.entity.PmBriefingTemplate;
import com.mido.pm.briefing.mapper.PmBriefingTemplateMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 简报模板服务：惰性为租户生成内置日/周/月报模板，并提供列表/详情。
 */
@Service
public class BriefingTemplateService {

    private static final String DAILY_SCHEMA =
            "[{\"key\":\"todayDone\",\"label\":\"今日完成\",\"type\":\"textarea\"},"
            + "{\"key\":\"tomorrowPlan\",\"label\":\"明日计划\",\"type\":\"textarea\"},"
            + "{\"key\":\"problem\",\"label\":\"遇到问题\",\"type\":\"textarea\"}]";
    private static final String WEEKLY_SCHEMA =
            "[{\"key\":\"weekDone\",\"label\":\"本周完成\",\"type\":\"textarea\"},"
            + "{\"key\":\"nextWeekPlan\",\"label\":\"下周计划\",\"type\":\"textarea\"},"
            + "{\"key\":\"riskIssue\",\"label\":\"问题与风险\",\"type\":\"textarea\"}]";
    private static final String MONTHLY_SCHEMA =
            "[{\"key\":\"monthSummary\",\"label\":\"本月总结\",\"type\":\"textarea\"},"
            + "{\"key\":\"nextMonthPlan\",\"label\":\"下月计划\",\"type\":\"textarea\"},"
            + "{\"key\":\"suggestion\",\"label\":\"问题与建议\",\"type\":\"textarea\"}]";

    private final PmBriefingTemplateMapper templateMapper;

    public BriefingTemplateService(PmBriefingTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    /** 模板列表（惰性建内置日/周/月报）。 */
    public List<BriefingTemplateVO> list() {
        ensureBuiltins();
        return templateMapper.selectList(Wrappers.<PmBriefingTemplate>lambdaQuery()
                        .eq(PmBriefingTemplate::getStatus, "active")
                        .orderByAsc(PmBriefingTemplate::getId))
                .stream().map(this::toVO).toList();
    }

    public BriefingTemplateVO get(Long id) {
        PmBriefingTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        return toVO(t);
    }

    /** 校验模板存在并返回，供 BriefingService 复用。 */
    public PmBriefingTemplate require(Long id) {
        PmBriefingTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        return t;
    }

    @Transactional(rollbackFor = Exception.class)
    public void ensureBuiltins() {
        Long count = templateMapper.selectCount(Wrappers.<PmBriefingTemplate>lambdaQuery()
                .eq(PmBriefingTemplate::getIsBuiltin, 1));
        if (count != null && count > 0) {
            return;
        }
        insertBuiltin("日报", "daily", DAILY_SCHEMA);
        insertBuiltin("周报", "weekly", WEEKLY_SCHEMA);
        insertBuiltin("月报", "monthly", MONTHLY_SCHEMA);
    }

    private void insertBuiltin(String name, String type, String schema) {
        PmBriefingTemplate t = new PmBriefingTemplate();
        t.setName(name);
        t.setType(type);
        t.setSchema(schema);
        t.setScope("tenant");
        t.setIsBuiltin(1);
        t.setStatus("active");
        templateMapper.insert(t);
    }

    private BriefingTemplateVO toVO(PmBriefingTemplate t) {
        return new BriefingTemplateVO(t.getId(), t.getName(), t.getType(), parseFields(t.getSchema()), t.getStatus());
    }

    private List<FieldDefVO> parseFields(String schema) {
        List<FieldDefVO> fields = new ArrayList<>();
        if (schema == null || schema.isBlank()) {
            return fields;
        }
        JSONArray arr = JSONUtil.parseArray(schema);
        for (Object o : arr) {
            JSONObject obj = (JSONObject) o;
            fields.add(new FieldDefVO(obj.getStr("key"), obj.getStr("label"), obj.getStr("type")));
        }
        return fields;
    }
}
