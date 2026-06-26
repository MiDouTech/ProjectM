package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.task.domain.TaskStatus;
import com.mido.pm.task.domain.TaskWorkflow;
import com.mido.pm.task.entity.PmWorkflowTransition;
import com.mido.pm.task.mapper.PmWorkflowTransitionMapper;
import org.springframework.stereotype.Service;

/**
 * 工作流引擎：状态流转以「类型 × from_status × to_status」转移矩阵为准（取代硬编码 TaskWorkflow）。
 *
 * <p>双轨：默认任务类型及等价矩阵仅自用租户已种子；当租户未配置工作流元数据
 * （无默认类型 / 状态库缺该状态）时，回落到 {@link TaskWorkflow}，保证未升级租户行为不变。
 * id 解析统一委托 {@link WorkItemMetaResolver}。</p>
 */
@Service
public class WorkflowEngine {

    private final WorkItemMetaResolver resolver;
    private final PmWorkflowTransitionMapper transitionMapper;

    public WorkflowEngine(WorkItemMetaResolver resolver, PmWorkflowTransitionMapper transitionMapper) {
        this.resolver = resolver;
        this.transitionMapper = transitionMapper;
    }

    /**
     * 校验默认任务类型下的状态流转（供 TaskService.changeStatus 调用）。
     * 已配置工作流的租户走矩阵；未配置则回落 TaskWorkflow（按 TaskStatus 枚举）。
     */
    public void assertTaskTransit(String fromCode, String toCode) {
        Long typeId = resolver.defaultTaskTypeId();
        Long fromId = typeId == null ? null : resolver.statusIdByName(fromCode);
        Long toId = typeId == null ? null : resolver.statusIdByName(toCode);
        if (typeId == null || fromId == null || toId == null) {
            TaskWorkflow.assertTransit(TaskStatus.fromCode(fromCode), TaskStatus.fromCode(toCode));
            return;
        }
        if (!canTransit(typeId, fromId, toId)) {
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
}
