package com.mido.pm.task.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.task.dto.PriorityLevelDTO;
import com.mido.pm.task.dto.PriorityModeSaveDTO;
import com.mido.pm.task.entity.PmPriorityLevel;
import com.mido.pm.task.entity.PmPriorityMode;
import com.mido.pm.task.mapper.PmPriorityLevelMapper;
import com.mido.pm.task.mapper.PmPriorityModeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 优先级模式服务单测：创建写档位、内置不可删。
 */
@ExtendWith(MockitoExtension.class)
class PriorityModeServiceTest {

    @Mock private PmPriorityModeMapper modeMapper;
    @Mock private PmPriorityLevelMapper levelMapper;

    private PriorityModeService service() {
        return new PriorityModeService(modeMapper, levelMapper);
    }

    @Test
    void createPersistsModeAndLevels() {
        service().create(new PriorityModeSaveDTO("缺陷优先级", null, null, List.of(
                new PriorityLevelDTO(null, "致命", "danger", 1, 0),
                new PriorityLevelDTO(null, "一般", "warning", 2, 1))));
        verify(modeMapper).insert(any(PmPriorityMode.class));
        verify(levelMapper).delete(any());
        verify(levelMapper, times(2)).insert(any(PmPriorityLevel.class));
    }

    @Test
    void deleteBuiltinRejected() {
        PmPriorityMode builtin = new PmPriorityMode();
        builtin.setId(1L);
        builtin.setBuiltin(1);
        when(modeMapper.selectById(1L)).thenReturn(builtin);
        assertThrows(BizException.class, () -> service().delete(1L));
        verify(modeMapper, never()).deleteById(any(java.io.Serializable.class));
    }
}
