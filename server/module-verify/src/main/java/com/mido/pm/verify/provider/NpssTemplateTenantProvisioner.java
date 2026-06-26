package com.mido.pm.verify.provider;

import com.mido.pm.common.tenant.TenantProvisionContext;
import com.mido.pm.common.tenant.TenantProvisioner;
import com.mido.pm.verify.entity.PmNpssSubjectTemplate;
import com.mido.pm.verify.mapper.PmNpssSubjectTemplateMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * NPSS 验收域租户播种（order=40）：为新租户建默认评价主体模板，使 NPSS「开箱即用」
 * （此前无内置默认，新租户首次发起价值验收需先手配主体，见 core-business-flow 体检断点8）。
 * 默认取通用模板：发起人30/业务方30/团队10/财务10/其他20——发起人+业务方为受益方(合计60≥50%)、
 * 启用合计=100%，满足 {@code SubjectWeightValidator} 硬校验；租户可在「NPSS 评价设置」改。
 */
@Component
public class NpssTemplateTenantProvisioner implements TenantProvisioner {

    /** {name, weight, beneficiary} 默认主体（npss-rule §2 战略级模板，作通用默认）。 */
    private static final Object[][] DEFAULTS = {
            {"发起人", 30, 1},
            {"业务方", 30, 1},
            {"团队", 10, 0},
            {"财务", 10, 0},
            {"其他", 20, 0},
    };

    private final PmNpssSubjectTemplateMapper templateMapper;

    public NpssTemplateTenantProvisioner(PmNpssSubjectTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public void provision(TenantProvisionContext ctx) {
        int sort = 0;
        for (Object[] d : DEFAULTS) {
            PmNpssSubjectTemplate t = new PmNpssSubjectTemplate();
            t.setName((String) d[0]);
            t.setWeight(BigDecimal.valueOf((Integer) d[1]));
            t.setBeneficiary((Integer) d[2]);
            t.setSort(sort++);
            t.setEnabled(1);
            templateMapper.insert(t);
        }
    }
}
