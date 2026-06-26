package com.mido.pm.task.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.task.dto.TransitionDTO;
import com.mido.pm.task.dto.WorkItemTypeSaveDTO;
import com.mido.pm.task.entity.PmWorkItemType;
import com.mido.pm.task.entity.PmWorkItemTransition;
import com.mido.pm.task.mapper.PmWorkItemTypeFieldMapper;
import com.mido.pm.task.mapper.PmWorkItemTypeMapper;
import com.mido.pm.task.mapper.PmWorkItemTransitionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作项类型服务单测：编码查重、内置不可删、流转矩阵先清后插。
 */
@ExtendWith(MockitoExtension.class)
class WorkItemTypeServiceTest {

    @Mock private PmWorkItemTypeMapper typeMapper;
    @Mock private PmWorkItemTypeFieldMapper typeFieldMapper;
    @Mock private PmWorkItemTransitionMapper transitionMapper;

    private WorkItemTypeService service() {
        return new WorkItemTypeService(typeMapper, typeFieldMapper, transitionMapper);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(typeMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BizException.class, () ->
                service().create(new WorkItemTypeSaveDTO("bug", "缺陷", "IT", 1, null)));
        verify(typeMapper, never()).insert(any(PmWorkItemType.class));
    }

    @Test
    void deleteBuiltinRejected() {
        PmWorkItemType builtin = new PmWorkItemType();
        builtin.setId(1L);
        builtin.setBuiltin(1);
        when(typeMapper.selectById(1L)).thenReturn(builtin);
        assertThrows(BizException.class, () -> service().delete(1L));
        verify(typeMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void saveTransitionsReplacesMatrix() {
        PmWorkItemType t = new PmWorkItemType();
        t.setId(5L);
        when(typeMapper.selectById(5L)).thenReturn(t);
        service().saveTransitions(5L, List.of(new TransitionDTO(1L, 2L), new TransitionDTO(2L, 3L)));
        verify(transitionMapper).delete(any());
        verify(transitionMapper, times(2)).insert(any(PmWorkItemTransition.class));
    }
}
