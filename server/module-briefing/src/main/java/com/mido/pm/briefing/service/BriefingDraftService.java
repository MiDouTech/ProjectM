package com.mido.pm.briefing.service;

import com.mido.pm.briefing.dto.BriefingTemplateVO;
import com.mido.pm.briefing.provider.BriefingDraftProvider;
import com.mido.pm.common.security.UserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 简报草稿生成：调 {@link BriefingDraftProvider} 汇总本人周期内工作，落入模板首字段供用户编辑。
 */
@Service
public class BriefingDraftService {

    private final BriefingTemplateService templateService;
    private final BriefingDraftProvider draftProvider;

    public BriefingDraftService(BriefingTemplateService templateService, BriefingDraftProvider draftProvider) {
        this.templateService = templateService;
        this.draftProvider = draftProvider;
    }

    /** 生成草稿内容（key→文本）：首字段填工作汇总，其余留空待填。 */
    public Map<String, Object> generate(Long templateId, LocalDate from, LocalDate to) {
        BriefingTemplateVO tpl = templateService.get(templateId);
        Map<String, Object> content = new LinkedHashMap<>();
        if (!tpl.fields().isEmpty()) {
            String summary = draftProvider.summarizeWork(UserContext.currentUserId(), from, to);
            content.put(tpl.fields().get(0).key(), summary);
        }
        return content;
    }
}
