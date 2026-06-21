package com.mido.pm.approval.outcome;

import com.mido.pm.approval.dto.ApprovalBizTypeVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * bizType 注册表单测：按 order 稳定排序输出 value/label；按 bizType 查 handler；未知返回 null。
 */
class ApprovalBizTypeRegistryTest {

    private ApprovalOutcomeHandler handler(String code, String label, int order) {
        ApprovalOutcomeHandler h = mock(ApprovalOutcomeHandler.class);
        when(h.bizType()).thenReturn(code);
        when(h.label()).thenReturn(label);
        when(h.order()).thenReturn(order);
        return h;
    }

    @Test
    void listSortedByOrder() {
        // 故意乱序注册，应按 order 升序输出
        ApprovalBizTypeRegistry reg = new ApprovalBizTypeRegistry(List.of(
                handler("change", "变更审批", 30),
                handler("project_init", "立项审批", 10),
                handler("cost", "费用审批", 20)));

        List<ApprovalBizTypeVO> list = reg.list();
        assertEquals(List.of("project_init", "cost", "change"),
                list.stream().map(ApprovalBizTypeVO::value).toList());
        assertEquals("立项审批", list.get(0).label());
    }

    @Test
    void handlerLookup() {
        ApprovalOutcomeHandler cost = handler("cost", "费用审批", 20);
        ApprovalBizTypeRegistry reg = new ApprovalBizTypeRegistry(List.of(cost));

        assertEquals(cost, reg.handler("cost"));
        assertNull(reg.handler("nope"));
        assertNull(reg.handler(null));
    }
}
