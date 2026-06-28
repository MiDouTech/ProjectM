package com.mido.pm.verify.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.change.entity.PmChangeRequest;
import com.mido.pm.verify.dto.SubjectTemplateDTO;
import com.mido.pm.verify.service.NpssSubjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * 评价主体模板变更应用器：反序列化 after_payload 并经 saveTemplates 回写租户模板。
 */
@ExtendWith(MockitoExtension.class)
class NpssSubjectTemplateChangeApplierTest {

    @Mock private NpssSubjectService npssSubjectService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void applyDeserializesAndSaves() {
        NpssSubjectTemplateChangeApplier applier =
                new NpssSubjectTemplateChangeApplier(npssSubjectService, objectMapper);
        assertTrue(applier.supports("npss_subject_template"));

        PmChangeRequest cr = new PmChangeRequest();
        cr.setBizType("npss_subject_template");
        cr.setAfterPayload("[{\"name\":\"受益方\",\"weight\":60,\"beneficiary\":true,\"sort\":0,\"enabled\":true},"
                + "{\"name\":\"其他\",\"weight\":40,\"beneficiary\":false,\"sort\":1,\"enabled\":true}]");

        applier.apply(cr);

        ArgumentCaptor<List<SubjectTemplateDTO>> cap = ArgumentCaptor.forClass(List.class);
        verify(npssSubjectService).saveTemplates(cap.capture());
        assertEquals(2, cap.getValue().size());
        assertEquals("受益方", cap.getValue().get(0).name());
    }
}
