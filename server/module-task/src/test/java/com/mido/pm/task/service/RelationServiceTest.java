package com.mido.pm.task.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.task.domain.RelationKind;
import com.mido.pm.task.dto.RelationCreateDTO;
import com.mido.pm.task.dto.TaskRelationVO;
import com.mido.pm.task.entity.PmRelation;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.mapper.PmRelationMapper;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作项关联服务单测：类型校验、自关联、目标存在、查重、双向列表。
 */
@ExtendWith(MockitoExtension.class)
class RelationServiceTest {

    @Mock private PmRelationMapper relationMapper;
    @Mock private PmTaskMapper taskMapper;

    private RelationService service() {
        return new RelationService(relationMapper, taskMapper);
    }

    private PmTask task(long id, String title, String status) {
        PmTask t = new PmTask();
        t.setId(id);
        t.setTitle(title);
        t.setStatus(status);
        return t;
    }

    @Test
    void linkRejectsInvalidKind() {
        assertThrows(BizException.class, () -> service().link(1L, new RelationCreateDTO(2L, "bogus")));
        verify(relationMapper, never()).insert(any(PmRelation.class));
    }

    @Test
    void linkRejectsSelf() {
        assertThrows(BizException.class, () -> service().link(1L, new RelationCreateDTO(1L, RelationKind.RELATED)));
    }

    @Test
    void linkRejectsMissingTarget() {
        when(taskMapper.selectById(1L)).thenReturn(task(1, "源", "未开始"));
        when(taskMapper.selectById(2L)).thenReturn(null);
        assertThrows(BizException.class, () -> service().link(1L, new RelationCreateDTO(2L, RelationKind.RELATED)));
    }

    @Test
    void linkPersistsWhenValid() {
        when(taskMapper.selectById(1L)).thenReturn(task(1, "源", "未开始"));
        when(taskMapper.selectById(2L)).thenReturn(task(2, "缺陷", "进行中"));
        when(relationMapper.selectCount(any())).thenReturn(0L);
        service().link(1L, new RelationCreateDTO(2L, RelationKind.RELATED));
        verify(relationMapper).insert(any(PmRelation.class));
    }

    @Test
    void listForTaskMarksDirectionAndResolvesOtherEnd() {
        PmRelation outgoing = new PmRelation();
        outgoing.setId(10L);
        outgoing.setRelationKind(RelationKind.RELATED);
        outgoing.setSourceTaskId(1L);
        outgoing.setTargetTaskId(2L);
        when(relationMapper.selectList(any())).thenReturn(List.of(outgoing));
        lenient().when(taskMapper.selectBatchIds(any())).thenReturn(List.of(task(2, "缺陷", "进行中")));

        List<TaskRelationVO> vos = service().listForTask(1L);
        assertEquals(1, vos.size());
        assertEquals("outgoing", vos.get(0).direction());
        assertEquals(2L, vos.get(0).relatedTaskId());
        assertEquals("缺陷", vos.get(0).relatedTitle());
    }
}
