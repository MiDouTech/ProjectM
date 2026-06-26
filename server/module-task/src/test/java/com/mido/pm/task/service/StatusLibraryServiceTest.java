package com.mido.pm.task.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.task.domain.MetaCategory;
import com.mido.pm.task.dto.StatusSaveDTO;
import com.mido.pm.task.entity.PmStatus;
import com.mido.pm.task.mapper.PmStatusMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 状态库服务单测：元类别校验、内置不可删。
 */
@ExtendWith(MockitoExtension.class)
class StatusLibraryServiceTest {

    @Mock private PmStatusMapper statusMapper;

    private StatusLibraryService service() {
        return new StatusLibraryService(statusMapper);
    }

    @Test
    void createRejectsInvalidMetaCategory() {
        assertThrows(BizException.class, () ->
                service().create(new StatusSaveDTO("测试中", "primary", "无效类别", "通用", 1, "active")));
        verify(statusMapper, never()).insert(any(PmStatus.class));
    }

    @Test
    void createPersistsValidStatus() {
        service().create(new StatusSaveDTO("测试中", "warning", MetaCategory.IN_PROGRESS, "通用", 5, null));
        verify(statusMapper).insert(any(PmStatus.class));
    }

    @Test
    void deleteBuiltinRejected() {
        PmStatus builtin = new PmStatus();
        builtin.setId(1L);
        builtin.setBuiltin(1);
        when(statusMapper.selectById(1L)).thenReturn(builtin);
        assertThrows(BizException.class, () -> service().delete(1L));
        verify(statusMapper, never()).deleteById(any(java.io.Serializable.class));
    }
}
