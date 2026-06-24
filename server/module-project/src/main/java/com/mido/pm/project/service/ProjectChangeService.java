package com.mido.pm.project.service;

import cn.hutool.json.JSONUtil;
import com.mido.pm.change.dto.ChangeRequestVO;
import com.mido.pm.change.dto.ChangeSubmitCmd;
import com.mido.pm.change.service.ChangeService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.project.dto.ProjectChangeRequestDTO;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.mapper.PmProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 项目时间（计划基线）变更发起：组装 before/after 快照后提交变更中心（{@link ChangeService}，复用审批引擎）。
 * 对标 Worktile「项目时间变更」。项目域负责字段快照与拟改值组装，变更域不耦合项目字段。
 * 日期以 ISO 字符串入快照，避免 JSON 序列化标度/格式歧义（与 ProjectScheduleChangeApplier 同源解析）。
 */
@Service
public class ProjectChangeService {

    /** 被改实体域标识（变更台账 biz_type / ChangeApplier.supports）。 */
    public static final String BIZ_TYPE = "project";

    private static final Set<String> CHANGE_TYPES = Set.of("project_schedule");

    /** 可变更基线字段：现值取值器(快照) + 拟改值取值器(DTO)，值均为 ISO 字符串。新增字段只动这一处。 */
    private record Field(String key, Function<PmProject, Object> current,
                         Function<ProjectChangeRequestDTO, Object> proposed) {
    }

    private static final List<Field> FIELDS = List.of(
            new Field("startDate", p -> iso(p.getStartDate()), r -> iso(r.startDate())),
            new Field("endDate", p -> iso(p.getEndDate()), r -> iso(r.endDate())));

    private final PmProjectMapper projectMapper;
    private final ChangeService changeService;

    public ProjectChangeService(PmProjectMapper projectMapper, ChangeService changeService) {
        this.projectMapper = projectMapper;
        this.changeService = changeService;
    }

    /** 发起项目时间变更：组装 before/after 后提交变更中心，返回变更单 id。 */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long projectId, ProjectChangeRequestDTO req) {
        if (!CHANGE_TYPES.contains(req.changeType())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法变更类型: " + req.changeType());
        }
        PmProject p = projectMapper.selectById(projectId);
        if (p == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        // 起止顺序守卫：以拟改值覆盖现值后的有效区间，结束不得早于开始
        LocalDate effStart = req.startDate() != null ? req.startDate() : p.getStartDate();
        LocalDate effEnd = req.endDate() != null ? req.endDate() : p.getEndDate();
        if (effStart != null && effEnd != null && effEnd.isBefore(effStart)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "结束日期不得早于开始日期");
        }
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();
        for (Field f : FIELDS) {
            Object cur = f.current().apply(p);
            before.put(f.key(), cur);
            Object proposed = f.proposed().apply(req);
            if (proposed != null && !proposed.equals(cur)) {
                after.put(f.key(), proposed);
            }
        }
        if (after.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "拟改值与现值一致，无变更内容");
        }
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("changeType", req.changeType());
        formData.put("projectId", projectId);
        formData.put("projectName", p.getName());

        ChangeSubmitCmd cmd = new ChangeSubmitCmd(BIZ_TYPE, projectId, req.changeType(),
                "项目时间变更·" + p.getName(), req.reason(), req.impact(),
                JSONUtil.toJsonStr(before), JSONUtil.toJsonStr(after), formData);
        return changeService.submit(cmd);
    }

    /** 某项目的变更历史（变更中心·项目视角）。 */
    public List<ChangeRequestVO> list(Long projectId) {
        return changeService.list(BIZ_TYPE, projectId, null);
    }

    private static Object iso(LocalDate d) {
        return d == null ? null : d.toString();
    }
}
