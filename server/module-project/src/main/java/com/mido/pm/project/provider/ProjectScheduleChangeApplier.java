package com.mido.pm.project.provider;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mido.pm.change.domain.ChangeApplier;
import com.mido.pm.change.entity.PmChangeRequest;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.mapper.PmProjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 项目时间变更应用器：变更审批通过（或免审）后，把 after_payload 的起止日期覆盖回 pm_project。
 * 直接经 projectMapper 写表（不走 ProjectService，避免与变更/审批编排成环）。
 * 不另发业务事件：回写写操作由 ChangeApplyService 同事务发 change.applied 覆盖（项目域无 project.updated 事件）。
 */
@Component
public class ProjectScheduleChangeApplier implements ChangeApplier {

    private static final Logger log = LoggerFactory.getLogger(ProjectScheduleChangeApplier.class);

    private final PmProjectMapper projectMapper;

    public ProjectScheduleChangeApplier(PmProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    @Override
    public boolean supports(String bizType) {
        return "project".equals(bizType);
    }

    @Override
    public void apply(PmChangeRequest request) {
        PmProject p = projectMapper.selectById(request.getBizId());
        if (p == null) {
            return;
        }
        JSONObject after = JSONUtil.parseObj(request.getAfterPayload() == null ? "{}" : request.getAfterPayload());
        boolean changed = false;
        for (String key : after.keySet()) {
            switch (key) {
                case "startDate" -> {
                    p.setStartDate(parseDate(after.getStr("startDate")));
                    changed = true;
                }
                case "endDate" -> {
                    p.setEndDate(parseDate(after.getStr("endDate")));
                    changed = true;
                }
                default -> log.warn("项目时间变更含未知字段，已忽略：changeId={}, field={}", request.getId(), key);
            }
        }
        if (changed) {
            projectMapper.updateById(p);
        }
    }

    private LocalDate parseDate(String iso) {
        return iso == null || iso.isBlank() ? null : LocalDate.parse(iso);
    }
}
