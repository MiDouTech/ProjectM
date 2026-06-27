package com.mido.pm.approval.provider;

import com.mido.pm.approval.entity.ApprovalFlow;
import com.mido.pm.approval.mapper.ApprovalFlowMapper;
import com.mido.pm.common.tenant.TenantProvisionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** 审批域租户播种单测：5 条立项流 + 1 条费用流，均带中文展示名（与 V22 回填命名一致）。 */
@ExtendWith(MockitoExtension.class)
class ApprovalTenantProvisionerTest {

    @Mock
    private ApprovalFlowMapper flowMapper;

    @Test
    void seedsBuiltinFlowsWithChineseDisplayName() {
        TenantProvisionContext ctx = new TenantProvisionContext(1L, "t1", "租户一", "admin", "pwd");
        ctx.put(TenantProvisionContext.KEY_ADMIN_USER_ID, 99L);
        ArgumentCaptor<ApprovalFlow> captor = ArgumentCaptor.forClass(ApprovalFlow.class);

        new ApprovalTenantProvisioner(flowMapper).provision(ctx);

        verify(flowMapper, times(6)).insert(captor.capture());
        Map<String, String> byName = captor.getAllValues().stream()
                .collect(Collectors.toMap(ApprovalFlow::getName, ApprovalFlow::getDisplayName));
        assertEquals("战略级标准流程", byName.get("S_STANDARD"));
        assertEquals("创新级 POC 流程", byName.get("I_POC"));
        assertEquals("常规运营流程", byName.get("O_NORMAL"));
        assertEquals("定向整改流程", byName.get("O_RECTIFY"));
        assertEquals("专项督办流程", byName.get("O_SUPERVISE"));
        assertEquals("费用审批（默认）", byName.get("COST_DEFAULT"));
    }
}
