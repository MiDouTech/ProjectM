package com.mido.pm.task.template;

import com.mido.pm.project.template.TemplateConfig;
import com.mido.pm.task.dto.TaskCreateDTO;
import com.mido.pm.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** 模板任务骨架供给单测：按 phases[].tasks[] 建任务，stage=阶段名；空骨架不建。 */
@ExtendWith(MockitoExtension.class)
class TaskSkeletonProvisionerTest {

    @Mock
    private TaskService taskService;

    private TaskSkeletonProvisioner provisioner() {
        return new TaskSkeletonProvisioner(taskService);
    }

    @Test
    void createsOneTaskPerPhaseTaskWithStage() {
        TemplateConfig config = new TemplateConfig(List.of(
                new TemplateConfig.Phase("立项", List.of("填写立项申请", "干系人初稿")),
                new TemplateConfig.Phase("执行", List.of("执行任务"))),
                List.of(), null, null);

        ArgumentCaptor<TaskCreateDTO> captor = ArgumentCaptor.forClass(TaskCreateDTO.class);
        provisioner().provision(42L, config);

        verify(taskService, times(3)).create(captor.capture());
        TaskCreateDTO first = captor.getAllValues().get(0);
        assertEquals("填写立项申请", first.title());
        assertEquals(42L, first.projectId());
        assertEquals("立项", first.stage());
        assertEquals("执行", captor.getAllValues().get(2).stage());
    }

    @Test
    void nullOrEmptyPhasesCreateNothing() {
        provisioner().provision(1L, new TemplateConfig(null, List.of(), null, null));
        provisioner().provision(1L, null);
        verify(taskService, never()).create(any());
    }

    @Test
    void skipsBlankTaskTitles() {
        TemplateConfig config = new TemplateConfig(List.of(
                new TemplateConfig.Phase("阶段", java.util.Arrays.asList("有效", " ", null))),
                List.of(), null, null);
        provisioner().provision(1L, config);
        verify(taskService, times(1)).create(any());
    }
}
