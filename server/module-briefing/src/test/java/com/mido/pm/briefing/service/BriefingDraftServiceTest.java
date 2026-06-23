package com.mido.pm.briefing.service;

import com.mido.pm.briefing.dto.BriefingTemplateVO;
import com.mido.pm.briefing.dto.FieldDefVO;
import com.mido.pm.briefing.provider.BriefingDraftProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** 草稿生成单测：工作汇总落入模板首字段。 */
@ExtendWith(MockitoExtension.class)
class BriefingDraftServiceTest {

    @Mock private BriefingTemplateService templateService;
    @Mock private BriefingDraftProvider draftProvider;
    @InjectMocks private BriefingDraftService service;

    @Test
    void summaryFillsFirstField() {
        BriefingTemplateVO tpl = new BriefingTemplateVO(1L, "日报", "daily",
                List.of(new FieldDefVO("todayDone", "今日完成", "textarea"),
                        new FieldDefVO("tomorrowPlan", "明日计划", "textarea")),
                1, "active");
        when(templateService.get(1L)).thenReturn(tpl);
        when(draftProvider.summarizeWork(any(), any(), any())).thenReturn("- 任务A\n- 任务B");

        Map<String, Object> content = service.generate(1L, LocalDate.of(2026, 6, 23), LocalDate.of(2026, 6, 23));

        assertEquals("- 任务A\n- 任务B", content.get("todayDone"));
        assertEquals(1, content.size(), "仅首字段填充，其余留空");
    }

    @Test
    void emptyFieldsYieldsEmptyContent() {
        when(templateService.get(eq(2L)))
                .thenReturn(new BriefingTemplateVO(2L, "空", "daily", List.of(), 0, "active"));
        Map<String, Object> content = service.generate(2L, LocalDate.now(), LocalDate.now());
        assertEquals(0, content.size());
    }
}
