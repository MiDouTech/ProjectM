package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.task.domain.TaskStatus;
import com.mido.pm.task.domain.TaskWorkflow;
import com.mido.pm.task.entity.PmStatus;
import com.mido.pm.task.entity.PmWorkItemType;
import com.mido.pm.task.entity.PmWorkflowTransition;
import com.mido.pm.task.mapper.PmStatusMapper;
import com.mido.pm.task.mapper.PmWorkItemTypeMapper;
import com.mido.pm.task.mapper.PmWorkflowTransitionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工作流引擎：状态流转以「类型 × from_status × to_status」转移矩阵为准（取代硬编码 TaskWorkflow）。
 *
 * <p>阶段2 双轨：默认任务类型(code=task)及其等价矩阵仅自用租户已种子；
 * 当租户未配置工作流元数据（无默认类型 / 状态库缺该状态）时，回落到 {@link TaskWorkflow}，
 * 保证未升级租户行为不变。task 持久化 type_id/status_id 后（阶段3）可去除按名解析的兜底。</p>
 */
@Service
public class WorkflowEngine {

    /** 默认任务类型编码（承接存量任务） */
    public static final String DEFAULT_TASK_TYPE_CODE = "task";

    private final PmWorkItemTypeMapper typeMapper;
    private final PmStatusMapper statusMapper;
    private final PmWorkflowTransitionMapper transitionMapper;

    public WorkflowEngine(PmWorkItemTypeMapper typeMapper, PmStatusMapper statusMapper,
                          PmWorkflowTransitionMapper transitionMapper) {
        this.typeMapper = typeMapper;
        this.statusMapper = statusMapper;
        this.transitionMapper = transitionMapper;
    }

    /**
     * 校验默认任务类型下的状态流转（供 TaskService.changeStatus 调用）。
     * 已配置工作流的租户走矩阵；未配置则回落 TaskWorkflow（按 TaskStatus 枚举）。
     */
    public void assertTaskTransit(String fromCode, String toCode) {
        PmWorkItemType type = defaultTaskType();
        Long fromId = type == null ? null : statusIdByName(fromCode);
        Long toId = type == null ? null : statusIdByName(toCode);
        if (type == null || fromId == null || toId == null) {
            // 未升级/未配置：保持旧行为
            TaskWorkflow.assertTransit(TaskStatus.fromCode(fromCode), TaskStatus.fromCode(toCode));
            return;
        }
        if (!canTransit(type.getId(), fromId, toId)) {
            throw new BizException(ErrorCode.CONFLICT, "非法任务状态流转：" + fromCode + " → " + toCode);
        }
    }

    /** 某类型下 from→to 是否为合法转移（矩阵存在该行）。 */
    public boolean canTransit(Long typeId, Long fromStatusId, Long toStatusId) {
        if (typeId == null || fromStatusId == null || toStatusId == null) {
            return false;
        }
        Long c = transitionMapper.selectCount(Wrappers.<PmWorkflowTransition>lambdaQuery()
                .eq(PmWorkflowTransition::getTypeId, typeId)
                .eq(PmWorkflowTransition::getFromStatusId, fromStatusId)
                .eq(PmWorkflowTransition::getToStatusId, toStatusId));
        return c != null && c > 0;
    }

    private PmWorkItemType defaultTaskType() {
        List<PmWorkItemType> types = typeMapper.selectList(Wrappers.<PmWorkItemType>lambdaQuery()
                .eq(PmWorkItemType::getCode, DEFAULT_TASK_TYPE_CODE)
                .orderByAsc(PmWorkItemType::getId));
        return types.isEmpty() ? null : types.get(0);
    }

    private Long statusIdByName(String name) {
        if (name == null) {
            return null;
        }
        List<PmStatus> rows = statusMapper.selectList(Wrappers.<PmStatus>lambdaQuery()
                .eq(PmStatus::getName, name).orderByAsc(PmStatus::getId));
        return rows.isEmpty() ? null : rows.get(0).getId();
    }
}
