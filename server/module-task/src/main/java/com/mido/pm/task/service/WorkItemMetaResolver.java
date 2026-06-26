package com.mido.pm.task.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.task.domain.MetaCategory;
import com.mido.pm.task.entity.PmPriorityLevel;
import com.mido.pm.task.entity.PmPriorityMode;
import com.mido.pm.task.entity.PmStatus;
import com.mido.pm.task.entity.PmWorkItemType;
import com.mido.pm.task.mapper.PmPriorityLevelMapper;
import com.mido.pm.task.mapper.PmPriorityModeMapper;
import com.mido.pm.task.mapper.PmStatusMapper;
import com.mido.pm.task.mapper.PmWorkItemTypeMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工作项元数据解析器（阶段3）：把任务的字符串状态/优先级解析到状态库/类型/优先级模式的 id，
 * 供写路径双写与 {@link WorkflowEngine} 复用。所有解析 best-effort：租户未种子则返回 null。
 * 全部按当前租户查询（多租户拦截器注入 tenant_id）。
 */
@Service
public class WorkItemMetaResolver {

    /** 默认任务类型编码（承接存量任务） */
    public static final String DEFAULT_TASK_TYPE_CODE = "task";

    private final PmWorkItemTypeMapper typeMapper;
    private final PmStatusMapper statusMapper;
    private final PmPriorityModeMapper priorityModeMapper;
    private final PmPriorityLevelMapper priorityLevelMapper;

    public WorkItemMetaResolver(PmWorkItemTypeMapper typeMapper, PmStatusMapper statusMapper,
                                PmPriorityModeMapper priorityModeMapper, PmPriorityLevelMapper priorityLevelMapper) {
        this.typeMapper = typeMapper;
        this.statusMapper = statusMapper;
        this.priorityModeMapper = priorityModeMapper;
        this.priorityLevelMapper = priorityLevelMapper;
    }

    /** 默认任务类型 id；未种子返回 null。 */
    public Long defaultTaskTypeId() {
        List<PmWorkItemType> types = typeMapper.selectList(Wrappers.<PmWorkItemType>lambdaQuery()
                .eq(PmWorkItemType::getCode, DEFAULT_TASK_TYPE_CODE).orderByAsc(PmWorkItemType::getId));
        return types.isEmpty() ? null : types.get(0).getId();
    }

    /** 当前租户启用状态（按 sort 升序），供看板列/着色由状态库驱动；未配置返回空。 */
    public List<PmStatus> activeStatuses() {
        return statusMapper.selectList(Wrappers.<PmStatus>lambdaQuery()
                .eq(PmStatus::getStatus, "active")
                .orderByAsc(PmStatus::getSort).orderByAsc(PmStatus::getId));
    }

    /** 当前租户「已完成」元类别的全部状态 id（供报表/汇总按元类别判定完成；未种子返回空）。 */
    public List<Long> doneStatusIds() {
        return statusMapper.selectList(Wrappers.<PmStatus>lambdaQuery()
                        .eq(PmStatus::getMetaCategory, MetaCategory.DONE))
                .stream().map(PmStatus::getId).toList();
    }

    /** 状态名 → 状态库 id；未匹配返回 null。 */
    public Long statusIdByName(String name) {
        if (name == null) {
            return null;
        }
        List<PmStatus> rows = statusMapper.selectList(Wrappers.<PmStatus>lambdaQuery()
                .eq(PmStatus::getName, name).orderByAsc(PmStatus::getId));
        return rows.isEmpty() ? null : rows.get(0).getId();
    }

    /** 优先级值 → 内置优先级模式下对应档位 id；未匹配返回 null。 */
    public Long priorityLevelId(Integer priorityValue) {
        if (priorityValue == null) {
            return null;
        }
        List<PmPriorityMode> modes = priorityModeMapper.selectList(Wrappers.<PmPriorityMode>lambdaQuery()
                .eq(PmPriorityMode::getBuiltin, 1).orderByAsc(PmPriorityMode::getId));
        if (modes.isEmpty()) {
            return null;
        }
        List<PmPriorityLevel> levels = priorityLevelMapper.selectList(Wrappers.<PmPriorityLevel>lambdaQuery()
                .eq(PmPriorityLevel::getModeId, modes.get(0).getId())
                .eq(PmPriorityLevel::getLevelValue, priorityValue)
                .orderByAsc(PmPriorityLevel::getId));
        return levels.isEmpty() ? null : levels.get(0).getId();
    }
}
