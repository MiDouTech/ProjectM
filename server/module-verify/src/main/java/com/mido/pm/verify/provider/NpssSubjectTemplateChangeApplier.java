package com.mido.pm.verify.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.change.domain.ChangeApplier;
import com.mido.pm.change.entity.PmChangeRequest;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.verify.dto.SubjectTemplateDTO;
import com.mido.pm.verify.service.NpssSubjectService;
import com.mido.pm.verify.service.NpssSubjectTemplateChangeService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NPSS 评价主体模板变更应用器：变更通过（或免审）后，把 after_payload 覆盖回租户模板
 * （pm_npss_subject_template），经 {@link NpssSubjectService#saveTemplates} 落库（复用 §4 硬校验）。
 * 变更域不反向依赖验收域，回写经本 {@link ChangeApplier} 端口（与 GoalChangeApplier 同范式）。
 */
@Component
public class NpssSubjectTemplateChangeApplier implements ChangeApplier {

    private final NpssSubjectService npssSubjectService;
    private final ObjectMapper objectMapper;

    public NpssSubjectTemplateChangeApplier(NpssSubjectService npssSubjectService, ObjectMapper objectMapper) {
        this.npssSubjectService = npssSubjectService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String bizType) {
        return NpssSubjectTemplateChangeService.BIZ_TYPE.equals(bizType);
    }

    @Override
    public void apply(PmChangeRequest request) {
        String payload = request.getAfterPayload() == null ? "[]" : request.getAfterPayload();
        List<SubjectTemplateDTO> items;
        try {
            items = objectMapper.readValue(payload, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, SubjectTemplateDTO.class));
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "评价主体变更解析失败: " + e.getMessage());
        }
        npssSubjectService.saveTemplates(items);
    }
}
