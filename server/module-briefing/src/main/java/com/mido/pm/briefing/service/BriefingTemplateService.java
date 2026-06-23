package com.mido.pm.briefing.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.briefing.dto.AssignmentSaveDTO;
import com.mido.pm.briefing.dto.BriefingTemplateVO;
import com.mido.pm.briefing.dto.FieldDefVO;
import com.mido.pm.briefing.dto.TemplateSaveDTO;
import com.mido.pm.briefing.entity.PmBriefingAssignment;
import com.mido.pm.briefing.entity.PmBriefingTemplate;
import com.mido.pm.briefing.mapper.PmBriefingAssignmentMapper;
import com.mido.pm.briefing.mapper.PmBriefingTemplateMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
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
    private final PmBriefingAssignmentMapper assignmentMapper;

    public BriefingTemplateService(PmBriefingTemplateMapper templateMapper,
                                   PmBriefingAssignmentMapper assignmentMapper) {
        this.templateMapper = templateMapper;
        this.assignmentMapper = assignmentMapper;
    }

    /** 新建自定义模板（is_builtin=0）。 */
    @Transactional(rollbackFor = Exception.class)
    public Long create(TemplateSaveDTO dto) {
        PmBriefingTemplate t = new PmBriefingTemplate();
        t.setName(dto.name());
        t.setType(dto.type());
        t.setSchema(JSONUtil.toJsonStr(dto.fields() == null ? List.of() : dto.fields()));
        t.setScope("tenant");
        t.setIsBuiltin(0);
        t.setStatus("active");
        templateMapper.insert(t);
        return t.getId();
    }

    /** 更新自定义模板名称/字段（内置模板不可改）。 */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TemplateSaveDTO dto) {
        PmBriefingTemplate t = requireCustom(id);
        t.setName(dto.name());
        t.setType(dto.type());
        t.setSchema(JSONUtil.toJsonStr(dto.fields() == null ? List.of() : dto.fields()));
        templateMapper.updateById(t);
    }

    /** 停用自定义模板（内置模板不可停用）。 */
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        PmBriefingTemplate t = requireCustom(id);
        t.setStatus("disabled");
        templateMapper.updateById(t);
    }

    /** 设置模板指派（整体覆盖用户/部门）。 */
    @Transactional(rollbackFor = Exception.class)
    public void setAssignments(Long templateId, AssignmentSaveDTO dto) {
        require(templateId);
        assignmentMapper.delete(Wrappers.<PmBriefingAssignment>lambdaQuery()
                .eq(PmBriefingAssignment::getTemplateId, templateId));
        if (dto.userIds() != null) {
            dto.userIds().forEach(uid -> insertAssignment(templateId, "user", uid));
        }
        if (dto.deptIds() != null) {
            dto.deptIds().forEach(did -> insertAssignment(templateId, "dept", did));
        }
    }

    /** 我应交的模板：指派到我本人或我部门的 active 模板。 */
    public List<BriefingTemplateVO> assignedToMe() {
        CurrentUser u = UserContext.get();
        Long me = u == null ? null : u.getUserId();
        Long deptId = u == null ? null : u.getDeptId();
        List<PmBriefingAssignment> hits = assignmentMapper.selectList(
                Wrappers.<PmBriefingAssignment>lambdaQuery().and(w -> {
                    w.and(x -> x.eq(PmBriefingAssignment::getTargetType, "user").eq(PmBriefingAssignment::getTargetId, me));
                    if (deptId != null) {
                        w.or(x -> x.eq(PmBriefingAssignment::getTargetType, "dept").eq(PmBriefingAssignment::getTargetId, deptId));
                    }
                }));
        List<Long> templateIds = hits.stream().map(PmBriefingAssignment::getTemplateId).distinct().toList();
        if (templateIds.isEmpty()) {
            return List.of();
        }
        return templateMapper.selectList(Wrappers.<PmBriefingTemplate>lambdaQuery()
                        .in(PmBriefingTemplate::getId, templateIds)
                        .eq(PmBriefingTemplate::getStatus, "active"))
                .stream().map(this::toVO).toList();
    }

    private void insertAssignment(Long templateId, String type, Long targetId) {
        PmBriefingAssignment a = new PmBriefingAssignment();
        a.setTemplateId(templateId);
        a.setTargetType(type);
        a.setTargetId(targetId);
        assignmentMapper.insert(a);
    }

    private PmBriefingTemplate requireCustom(Long id) {
        PmBriefingTemplate t = require(id);
        if (Integer.valueOf(1).equals(t.getIsBuiltin())) {
            throw new BizException(ErrorCode.CONFLICT, "内置模板不可修改/停用");
        }
        return t;
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
        return new BriefingTemplateVO(t.getId(), t.getName(), t.getType(), parseFields(t.getSchema()),
                t.getIsBuiltin(), t.getStatus());
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
