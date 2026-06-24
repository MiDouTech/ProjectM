package com.mido.pm.cost.service;

import com.mido.pm.approval.service.ApprovalFlowService;
import com.mido.pm.approval.service.ApprovalService;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.cost.dto.CostCreateDTO;
import com.mido.pm.cost.entity.PmCost;
import com.mido.pm.cost.mapper.PmCostMapper;
import com.mido.pm.project.dto.ProjectVO;
import com.mido.pm.project.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 费用服务单测（mock，无 DB）：累计实际 &gt; 项目预算时发 cost.exceeded.budget 与 project.budget.exceeded；
 * 未超预算或未设预算时不发；预警不阻断（费用照常落库）。
 */
@ExtendWith(MockitoExtension.class)
class CostServiceTest {

    @Mock private PmCostMapper costMapper;
    @Mock private ProjectService projectService;
    @Mock private ApprovalService approvalService;
    @Mock private ApprovalFlowService approvalFlowService;
    @Mock private DomainEventPublisher eventPublisher;
    @InjectMocks private CostService service;

    private ProjectVO projectWithBudget(BigDecimal budget) {
        return new ProjectVO(100L, "P-1", "项目", null, "O", null, null, null, "进行中",
                budget, null, null, null, null, null, null, null, null, null);
    }

    private PmCost actual(String hours) {
        PmCost c = new PmCost();
        c.setProjectId(100L);
        c.setActualAmount(new BigDecimal(hours));
        return c;
    }

    private CostCreateDTO dto() {
        return new CostCreateDTO(100L, "餐费", "餐费", new BigDecimal("500"),
                new BigDecimal("600"), null, null);
    }

    @Test
    void exceedingBudgetEmitsBothEvents() {
        when(projectService.get(100L)).thenReturn(projectWithBudget(new BigDecimal("1000")));
        // 累计实际 600 + 500 = 1100 > 预算 1000
        when(costMapper.selectList(any())).thenReturn(List.of(actual("600"), actual("500")));

        service.create(dto());

        verify(costMapper).insert(any(PmCost.class)); // 不阻断：照常落库
        verify(eventPublisher).publish(eq("cost.exceeded.budget"), any());
        verify(eventPublisher).publish(eq("project.budget.exceeded"), any());
    }

    @Test
    void withinBudgetEmitsNoEvent() {
        when(projectService.get(100L)).thenReturn(projectWithBudget(new BigDecimal("1000")));
        when(costMapper.selectList(any())).thenReturn(List.of(actual("600")));

        service.create(dto());

        verify(costMapper).insert(any(PmCost.class));
        verify(eventPublisher, never()).publish(eq("cost.exceeded.budget"), any());
        verify(eventPublisher, never()).publish(eq("project.budget.exceeded"), any());
    }

    @Test
    void noBudgetSkipsCheck() {
        when(projectService.get(100L)).thenReturn(projectWithBudget(null));

        service.create(dto());

        verify(costMapper).insert(any(PmCost.class));
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void returnedCostCanBeResubmitted() {
        // 被退回非终态：可再次提报，发起新审批实例并发 cost.submitted
        PmCost returned = new PmCost();
        returned.setId(50L);
        returned.setProjectId(100L);
        returned.setStatus("被退回");
        returned.setBudgetAmount(new BigDecimal("500"));
        when(costMapper.selectById(50L)).thenReturn(returned);
        when(approvalFlowService.resolveFlowId("COST_DEFAULT")).thenReturn(6L);
        when(approvalService.submit(any())).thenReturn(900L);

        Long instanceId = service.submit(50L);

        assertEquals(900L, instanceId);
        verify(eventPublisher).publish(eq("cost.submitted"), any());
    }
}
