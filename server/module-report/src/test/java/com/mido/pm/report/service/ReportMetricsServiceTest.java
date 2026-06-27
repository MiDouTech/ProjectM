package com.mido.pm.report.service;

import com.mido.pm.project.service.ProjectService;
import com.mido.pm.report.domain.StatusMetaPort;
import com.mido.pm.report.dto.ProjectHealthVO;
import com.mido.pm.report.mapper.ReportMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** 项目健康度回归：已立预算但未录实际成本（早期态）时不应 NPE，预算使用率回落为 null。 */
@ExtendWith(MockitoExtension.class)
class ReportMetricsServiceTest {

    @Mock
    private ReportMapper reportMapper;
    @Mock
    private ProjectService projectService;
    @Mock
    private StatusMetaPort statusMetaPort;

    @Test
    void projectHealthHandlesNullActualCost() {
        when(statusMetaPort.doneStatusIds()).thenReturn(List.of());
        when(projectService.myVisibleProjectIds()).thenReturn(List.of());
        when(reportMapper.taskCountsByProject(eq(1L), any()))
                .thenReturn(Map.of("total", 0L, "completed", 0L, "overdue", 0L));
        Map<String, Object> budgetRow = new HashMap<>();
        budgetRow.put("budget", new BigDecimal("900000"));
        budgetRow.put("actualCost", null); // 已立预算、未录实际成本
        when(reportMapper.projectBudget(1L)).thenReturn(budgetRow);

        ReportMetricsService service = new ReportMetricsService(reportMapper, projectService, statusMetaPort);

        ProjectHealthVO vo = service.projectHealth(1L); // 不应抛 NPE
        assertEquals(1L, vo.projectId());
        assertNull(vo.budgetUsage(), "实际成本为空时预算使用率应为 null");
    }
}
