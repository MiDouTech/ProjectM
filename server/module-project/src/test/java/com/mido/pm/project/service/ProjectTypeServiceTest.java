package com.mido.pm.project.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.project.dto.ProjectTypeSaveDTO;
import com.mido.pm.project.entity.PmProjectType;
import com.mido.pm.project.event.ProjectTypeEvents;
import com.mido.pm.project.mapper.PmProjectTypeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 项目类型服务单测：唯一性校验、默认值兜底、启停事件。
 */
@ExtendWith(MockitoExtension.class)
class ProjectTypeServiceTest {

    @Mock private PmProjectTypeMapper typeMapper;
    @Mock private DomainEventPublisher eventPublisher;

    private ProjectTypeService service() {
        return new ProjectTypeService(typeMapper, eventPublisher);
    }

    private ProjectTypeSaveDTO dto(String code) {
        return new ProjectTypeSaveDTO(code, "战略级", null, "danger", null, 10,
                "L3", 1, 1L, null, "年度重点");
    }

    @Test
    void createRejectsDuplicateCode() {
        when(typeMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BizException.class, () -> service().create(dto("S")));
        verify(typeMapper, never()).insert(any(PmProjectType.class));
    }

    @Test
    void createPersistsAndPublishesEvent() {
        when(typeMapper.selectCount(any())).thenReturn(0L);
        service().create(dto("S"));
        verify(typeMapper).insert(any(PmProjectType.class));
        verify(eventPublisher).publish(eq(ProjectTypeEvents.CREATED), any());
    }

    @Test
    void createDefaultsRequiresNpssAndSortWhenNull() {
        when(typeMapper.selectCount(any())).thenReturn(0L);
        ProjectTypeSaveDTO d = new ProjectTypeSaveDTO("X", "类型X", null, null, null, null,
                null, null, null, null, null);
        ArgumentCaptor<PmProjectType> captor = ArgumentCaptor.forClass(PmProjectType.class);
        service().create(d);
        verify(typeMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getRequiresNpss());
        assertEquals(0, captor.getValue().getSort());
        assertEquals(ProjectTypeService.STATUS_ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void updateRejectsWhenMissing() {
        when(typeMapper.selectById(9L)).thenReturn(null);
        assertThrows(BizException.class, () -> service().update(9L, dto("S")));
        verify(typeMapper, never()).updateById(any(PmProjectType.class));
    }

    @Test
    void disablePublishesDisabledEvent() {
        PmProjectType t = new PmProjectType();
        t.setId(1L);
        t.setCode("S");
        when(typeMapper.selectById(1L)).thenReturn(t);
        service().setStatus(1L, false);
        assertEquals(ProjectTypeService.STATUS_DISABLED, t.getStatus());
        verify(eventPublisher).publish(eq(ProjectTypeEvents.DISABLED), any());
    }

    @Test
    void enablePublishesUpdatedEvent() {
        PmProjectType t = new PmProjectType();
        t.setId(1L);
        t.setCode("S");
        when(typeMapper.selectById(1L)).thenReturn(t);
        service().setStatus(1L, true);
        assertEquals(ProjectTypeService.STATUS_ACTIVE, t.getStatus());
        verify(eventPublisher).publish(eq(ProjectTypeEvents.UPDATED), any());
    }
}
