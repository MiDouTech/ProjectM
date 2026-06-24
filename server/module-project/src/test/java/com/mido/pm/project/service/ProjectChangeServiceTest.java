package com.mido.pm.project.service;

import com.mido.pm.change.dto.ChangeSubmitCmd;
import com.mido.pm.change.service.ChangeService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.project.dto.ProjectChangeRequestDTO;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.mapper.PmProjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 项目时间变更发起单测：组装 after 并委托变更中心；非法类型/无变更/起止逆序拒绝。 */
@ExtendWith(MockitoExtension.class)
class ProjectChangeServiceTest {

    @Mock
    private PmProjectMapper projectMapper;
    @Mock
    private ChangeService changeService;
    @InjectMocks
    private ProjectChangeService service;

    private PmProject project() {
        PmProject p = new PmProject();
        p.setId(1L);
        p.setName("项目A");
        p.setStartDate(LocalDate.of(2026, 6, 1));
        p.setEndDate(LocalDate.of(2026, 6, 30));
        return p;
    }

    @Test
    void submitBuildsAfterAndDelegates() {
        when(projectMapper.selectById(1L)).thenReturn(project());
        when(changeService.submit(any())).thenReturn(7L);

        Long id = service.submit(1L, new ProjectChangeRequestDTO(
                "project_schedule", "工期顺延", null, null, LocalDate.of(2026, 7, 15)));

        assertEquals(7L, id);
        ArgumentCaptor<ChangeSubmitCmd> cmd = ArgumentCaptor.forClass(ChangeSubmitCmd.class);
        verify(changeService).submit(cmd.capture());
        assertEquals("project", cmd.getValue().bizType());
        assertTrue(cmd.getValue().afterPayload().contains("endDate"), "after 应含 endDate");
        assertTrue(cmd.getValue().afterPayload().contains("2026-07-15"));
    }

    @Test
    void rejectsWhenNoEffectiveChange() {
        when(projectMapper.selectById(1L)).thenReturn(project());
        // 拟改值与现值一致
        assertThrows(BizException.class, () -> service.submit(1L, new ProjectChangeRequestDTO(
                "project_schedule", "无", null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30))));
        verify(changeService, never()).submit(any());
    }

    @Test
    void rejectsWhenEndBeforeStart() {
        when(projectMapper.selectById(1L)).thenReturn(project());
        assertThrows(BizException.class, () -> service.submit(1L, new ProjectChangeRequestDTO(
                "project_schedule", "逆序", null, null, LocalDate.of(2026, 5, 1))));
        verify(changeService, never()).submit(any());
    }

    @Test
    void rejectsIllegalChangeType() {
        assertThrows(BizException.class, () -> service.submit(1L, new ProjectChangeRequestDTO(
                "bogus", "x", null, null, LocalDate.of(2026, 7, 1))));
        verify(changeService, never()).submit(any());
    }
}
