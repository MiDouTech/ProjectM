package com.mido.pm.task.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.task.dto.CriticalPathVO;
import com.mido.pm.task.dto.DependencyCreateDTO;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.entity.PmTaskDependency;
import com.mido.pm.task.mapper.PmTaskDependencyMapper;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 任务依赖服务单测（mock mapper，无 DB）：新增成环则拒绝、跨项目拒绝、无环则落库；关键路径按工期接线。
 */
@ExtendWith(MockitoExtension.class)
class TaskDependencyServiceTest {

    @Mock private PmTaskDependencyMapper depMapper;
    @Mock private PmTaskMapper taskMapper;
    @InjectMocks private TaskDependencyService service;

    private PmTask task(long id, long projectId) {
        PmTask t = new PmTask();
        t.setId(id);
        t.setProjectId(projectId);
        return t;
    }

    private PmTask task(long id, long projectId, LocalDate start, LocalDate due) {
        PmTask t = task(id, projectId);
        t.setStartDate(start);
        t.setDueDate(due);
        return t;
    }

    private PmTaskDependency dep(long pred, long succ) {
        PmTaskDependency d = new PmTaskDependency();
        d.setPredecessorId(pred);
        d.setSuccessorId(succ);
        d.setType("FS");
        return d;
    }

    @Test
    void addRejectsCycle() {
        // 现有 1->2->3，新增 3->1 成环
        when(taskMapper.selectById(3L)).thenReturn(task(3, 100));
        when(taskMapper.selectById(1L)).thenReturn(task(1, 100));
        when(taskMapper.selectList(any())).thenReturn(List.of(task(1, 100), task(2, 100), task(3, 100)));
        when(depMapper.selectCount(any())).thenReturn(0L);
        when(depMapper.selectList(any())).thenReturn(List.of(dep(1, 2), dep(2, 3)));

        BizException ex = assertThrows(BizException.class,
                () -> service.add(new DependencyCreateDTO(3L, 1L, null)));
        assertTrue(ex.getMessage().contains("循环依赖"));
        verify(depMapper, never()).insert(any(PmTaskDependency.class));
    }

    @Test
    void addRejectsCrossProject() {
        when(taskMapper.selectById(1L)).thenReturn(task(1, 100));
        when(taskMapper.selectById(2L)).thenReturn(task(2, 200));

        assertThrows(BizException.class, () -> service.add(new DependencyCreateDTO(1L, 2L, null)));
        verify(depMapper, never()).insert(any(PmTaskDependency.class));
    }

    @Test
    void addInsertsWhenAcyclic() {
        // 现有 1->2，新增 1->3 不成环
        when(taskMapper.selectById(1L)).thenReturn(task(1, 100));
        when(taskMapper.selectById(3L)).thenReturn(task(3, 100));
        when(taskMapper.selectList(any())).thenReturn(List.of(task(1, 100), task(2, 100), task(3, 100)));
        when(depMapper.selectCount(any())).thenReturn(0L);
        when(depMapper.selectList(any())).thenReturn(List.of(dep(1, 2)));

        service.add(new DependencyCreateDTO(1L, 3L, null));

        verify(depMapper).insert(any(PmTaskDependency.class));
    }

    @Test
    void criticalPathFromDatesAndDeps() {
        // 链 1(5天)->2(3天)，工期 8，全关键
        when(taskMapper.selectList(any())).thenReturn(List.of(
                task(1, 100, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 6)),
                task(2, 100, LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 9))));
        lenient().when(depMapper.selectList(any())).thenReturn(List.of(dep(1, 2)));

        CriticalPathVO vo = service.criticalPath(100L);

        assertEquals(8L, vo.totalDurationDays());
        assertEquals(List.of(1L, 2L), vo.criticalTaskIds());
    }
}
