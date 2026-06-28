package com.mido.pm.verify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.change.dto.ChangeSubmitCmd;
import com.mido.pm.change.service.ChangeService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.verify.dto.SubjectTemplateDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 租户级评价主体模板变更发起：校验通过则提交变更中心（bizType=npss_subject_template），校验失败不提交。
 */
@ExtendWith(MockitoExtension.class)
class NpssSubjectTemplateChangeServiceTest {

    @Mock private NpssSubjectService npssSubjectService;
    @Mock private ChangeService changeService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private NpssSubjectTemplateChangeService service;

    private static SubjectTemplateDTO t(String name, String w, boolean ben, boolean enabled) {
        return new SubjectTemplateDTO(null, name, new BigDecimal(w), ben, 0, enabled);
    }

    @Test
    void submitValidGoesToChangeCenter() {
        when(npssSubjectService.listTemplates()).thenReturn(List.of());
        service.submit(List.of(t("受益方", "60", true, true), t("其他", "40", false, true)));
        ArgumentCaptor<ChangeSubmitCmd> cap = ArgumentCaptor.forClass(ChangeSubmitCmd.class);
        verify(changeService).submit(cap.capture());
        assertEquals(NpssSubjectTemplateChangeService.BIZ_TYPE, cap.getValue().bizType());
        assertEquals(NpssSubjectTemplateChangeService.CONFIG_BIZ_ID, cap.getValue().bizId());
    }

    @Test
    void submitRejectWeightsNot100() {
        assertThrows(BizException.class, () -> service.submit(List.of(
                t("受益方", "60", true, true), t("其他", "30", false, true))));
        verify(changeService, never()).submit(any());
    }
}
