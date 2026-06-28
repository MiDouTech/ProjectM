package com.mido.pm.verify.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.change.dto.ChangeSubmitCmd;
import com.mido.pm.change.service.ChangeService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.verify.domain.SubjectWeightValidator;
import com.mido.pm.verify.domain.SubjectWeightValidator.SubjectWeight;
import com.mido.pm.verify.dto.SubjectTemplateDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户级 NPSS 评价主体模板「变更发起」：组装 before/after 快照提交变更中心（{@link ChangeService}）。
 * 默认无变更策略=免审即时回写（经 {@link com.mido.pm.verify.provider.NpssSubjectTemplateChangeApplier} 落库）；
 * 后续在变更策略为 changeType=npss_subject_template 配置 requireApproval+flowId 即自动改走审批，无需改代码。
 */
@Service
public class NpssSubjectTemplateChangeService {

    /** 变更台账 biz_type / ChangeApplier.supports。 */
    public static final String BIZ_TYPE = "npss_subject_template";
    /** 变更类型（变更策略 changeType；与 BIZ_TYPE 同名，单一配置对象）。 */
    public static final String CHANGE_TYPE = "npss_subject_template";
    /** 租户级配置无单实体 id，用固定 0 作 bizId（每租户一条 pending，tenant 由拦截器隔离）。 */
    public static final long CONFIG_BIZ_ID = 0L;

    private final NpssSubjectService npssSubjectService;
    private final ChangeService changeService;
    private final ObjectMapper objectMapper;

    public NpssSubjectTemplateChangeService(NpssSubjectService npssSubjectService,
                                            ChangeService changeService, ObjectMapper objectMapper) {
        this.npssSubjectService = npssSubjectService;
        this.changeService = changeService;
        this.objectMapper = objectMapper;
    }

    /** 发起评价主体模板变更：先做 §4 硬校验，再以 before/after 快照提交变更中心。返回变更单 id。 */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(List<SubjectTemplateDTO> items) {
        List<SubjectTemplateDTO> list = items == null ? List.of() : items;
        // 提交前置硬校验（启用主体合计=100%、受益方≥50%），避免审批通过后回写才失败
        SubjectWeightValidator.validate(list.stream()
                .filter(t -> !Boolean.FALSE.equals(t.enabled()))
                .map(t -> new SubjectWeight(t.weight(), Boolean.TRUE.equals(t.beneficiary())))
                .toList());

        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("changeType", CHANGE_TYPE);
        ChangeSubmitCmd cmd = new ChangeSubmitCmd(BIZ_TYPE, CONFIG_BIZ_ID, CHANGE_TYPE,
                "NPSS 评价主体/权重变更", null, null,
                toJson(npssSubjectService.listTemplates()), toJson(list), formData);
        return changeService.submit(cmd);
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "评价主体序列化失败: " + e.getMessage());
        }
    }
}
