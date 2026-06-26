package com.mido.pm.project.service;

import com.mido.pm.project.dto.ProjectComponentSaveDTO;
import com.mido.pm.project.entity.PmProjectComponent;
import com.mido.pm.project.mapper.PmComponentMapper;
import com.mido.pm.project.mapper.PmProjectComponentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 组件服务单测：项目组件整列表保存先清后插。
 */
@ExtendWith(MockitoExtension.class)
class ComponentServiceTest {

    @Mock private PmComponentMapper componentMapper;
    @Mock private PmProjectComponentMapper projectComponentMapper;

    private ComponentService service() {
        return new ComponentService(componentMapper, projectComponentMapper);
    }

    @Test
    void saveInstalledReplacesAndOrders() {
        service().saveInstalled(9L, List.of(
                new ProjectComponentSaveDTO("overview", "概览", null),
                new ProjectComponentSaveDTO("task", "任务", null)));
        verify(projectComponentMapper).delete(any());
        verify(projectComponentMapper, times(2)).insert(any(PmProjectComponent.class));
    }
}
