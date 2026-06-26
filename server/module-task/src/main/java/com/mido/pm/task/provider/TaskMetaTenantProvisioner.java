package com.mido.pm.task.provider;

import com.mido.pm.common.tenant.TenantProvisionContext;
import com.mido.pm.common.tenant.TenantProvisioner;
import com.mido.pm.task.entity.PmPriorityLevel;
import com.mido.pm.task.entity.PmPriorityMode;
import com.mido.pm.task.entity.PmStatus;
import com.mido.pm.task.entity.PmWorkItemTransition;
import com.mido.pm.task.entity.PmWorkItemType;
import com.mido.pm.task.mapper.PmPriorityLevelMapper;
import com.mido.pm.task.mapper.PmPriorityModeMapper;
import com.mido.pm.task.mapper.PmStatusMapper;
import com.mido.pm.task.mapper.PmWorkItemTransitionMapper;
import com.mido.pm.task.mapper.PmWorkItemTypeMapper;
import org.springframework.stereotype.Component;

/**
 * 任务域元数据租户播种（order=25）：状态库（未开始/进行中/已完成/已验收）、默认优先级模式（高/中/低）、
 * 默认任务类型 + 等价于 TaskWorkflow 的流转矩阵。与 V57/V58/V59 内置种子等价，但 id 走雪花、引用按本租户内捕获。
 */
@Component
public class TaskMetaTenantProvisioner implements TenantProvisioner {

    private final PmStatusMapper statusMapper;
    private final PmPriorityModeMapper priorityModeMapper;
    private final PmPriorityLevelMapper priorityLevelMapper;
    private final PmWorkItemTypeMapper typeMapper;
    private final PmWorkItemTransitionMapper transitionMapper;

    public TaskMetaTenantProvisioner(PmStatusMapper statusMapper, PmPriorityModeMapper priorityModeMapper,
                                     PmPriorityLevelMapper priorityLevelMapper, PmWorkItemTypeMapper typeMapper,
                                     PmWorkItemTransitionMapper transitionMapper) {
        this.statusMapper = statusMapper;
        this.priorityModeMapper = priorityModeMapper;
        this.priorityLevelMapper = priorityLevelMapper;
        this.typeMapper = typeMapper;
        this.transitionMapper = transitionMapper;
    }

    @Override
    public int order() {
        return 25;
    }

    @Override
    public void provision(TenantProvisionContext ctx) {
        Long s1 = status("未开始", "info", "未开始", 10);
        Long s2 = status("进行中", "primary", "进行中", 20);
        Long s3 = status("已完成", "success", "已完成", 30);
        Long s4 = status("已验收", "success", "已完成", 40);

        PmPriorityMode mode = new PmPriorityMode();
        mode.setName("默认优先级模式");
        mode.setRemark("高/中/低三档");
        mode.setBuiltin(1);
        mode.setStatus("active");
        priorityModeMapper.insert(mode);
        level(mode.getId(), "高", "danger", 1, 10);
        level(mode.getId(), "中", "warning", 2, 20);
        level(mode.getId(), "低", "info", 3, 30);

        PmWorkItemType type = new PmWorkItemType();
        type.setCode("task");
        type.setName("默认任务");
        type.setGroupName("通用");
        type.setBuiltin(1);
        type.setSort(10);
        type.setStatus("active");
        typeMapper.insert(type);
        Long t = type.getId();
        transition(t, s1, s2);   // 未开始 → 进行中
        transition(t, s2, s1);   // 进行中 → 未开始
        transition(t, s2, s3);   // 进行中 → 已完成
        transition(t, s3, s2);   // 已完成 → 进行中
        transition(t, s3, s4);   // 已完成 → 已验收
        transition(t, s4, s3);   // 已验收 → 已完成
    }

    private Long status(String name, String color, String metaCategory, int sort) {
        PmStatus s = new PmStatus();
        s.setName(name);
        s.setColor(color);
        s.setMetaCategory(metaCategory);
        s.setGroupName("通用");
        s.setSort(sort);
        s.setBuiltin(1);
        s.setStatus("active");
        statusMapper.insert(s);
        return s.getId();
    }

    private void level(Long modeId, String name, String color, int value, int sort) {
        PmPriorityLevel l = new PmPriorityLevel();
        l.setModeId(modeId);
        l.setName(name);
        l.setColor(color);
        l.setLevelValue(value);
        l.setSort(sort);
        priorityLevelMapper.insert(l);
    }

    private void transition(Long typeId, Long from, Long to) {
        PmWorkItemTransition tr = new PmWorkItemTransition();
        tr.setTypeId(typeId);
        tr.setFromStatusId(from);
        tr.setToStatusId(to);
        transitionMapper.insert(tr);
    }
}
