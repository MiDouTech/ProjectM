package com.mido.pm.briefing.service;

import com.mido.pm.briefing.dto.TemplateSaveDTO;
import com.mido.pm.briefing.entity.PmBriefingTemplate;
import com.mido.pm.briefing.mapper.PmBriefingAssignmentMapper;
import com.mido.pm.briefing.mapper.PmBriefingTemplateMapper;
import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 模板服务单测：新建自定义模板落 is_builtin=0；内置模板不可改/停用。 */
@ExtendWith(MockitoExtension.class)
class BriefingTemplateServiceTest {

    @Mock private PmBriefingTemplateMapper templateMapper;
    @Mock private PmBriefingAssignmentMapper assignmentMapper;
    @InjectMocks private BriefingTemplateService service;

    @Test
    void createInsertsCustomTemplate() {
        service.create(new TemplateSaveDTO("项目周报", "weekly", List.of()));
        verify(templateMapper).insert(any(PmBriefingTemplate.class));
    }

    @Test
    void updateBuiltinRejected() {
        PmBriefingTemplate builtin = new PmBriefingTemplate();
        builtin.setId(1L);
        builtin.setIsBuiltin(1);
        when(templateMapper.selectById(1L)).thenReturn(builtin);

        assertThrows(BizException.class,
                () -> service.update(1L, new TemplateSaveDTO("改名", "daily", List.of())));
        verify(templateMapper, never()).updateById(any(PmBriefingTemplate.class));
    }

    @Test
    void disableBuiltinRejected() {
        PmBriefingTemplate builtin = new PmBriefingTemplate();
        builtin.setIsBuiltin(1);
        when(templateMapper.selectById(1L)).thenReturn(builtin);
        assertThrows(BizException.class, () -> service.disable(1L));
    }
}
