package com.mido.pm.verify.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.verify.domain.SubjectWeightValidator;
import com.mido.pm.verify.domain.SubjectWeightValidator.SubjectWeight;
import com.mido.pm.verify.dto.ProjectSubjectDTO;
import com.mido.pm.verify.dto.ProjectSubjectVO;
import com.mido.pm.verify.dto.SubjectTemplateDTO;
import com.mido.pm.verify.dto.SubjectTemplateVO;
import com.mido.pm.verify.entity.PmNpssSubject;
import com.mido.pm.verify.entity.PmNpssSubjectMember;
import com.mido.pm.verify.entity.PmNpssSubjectTemplate;
import com.mido.pm.verify.mapper.PmNpssSubjectMapper;
import com.mido.pm.verify.mapper.PmNpssSubjectMemberMapper;
import com.mido.pm.verify.mapper.PmNpssSubjectTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * NPSS 评价方式配置（npss-rule §3A）：租户级评价主体模板 + 项目级评价主体（成员即干系人）。
 * 评价规则固定为"各主体加权求和"，仅配置主体与权重。整组提交（replace-all），保存即做硬校验（§4 主体口径）。
 * 不发领域事件：与 StakeholderService.saveWeights 同属配置写，沿用既有先例（domain-events 未注册配置事件，禁自造）。
 */
@Service
public class NpssSubjectService {

    private final PmNpssSubjectTemplateMapper templateMapper;
    private final PmNpssSubjectMapper subjectMapper;
    private final PmNpssSubjectMemberMapper memberMapper;

    public NpssSubjectService(PmNpssSubjectTemplateMapper templateMapper,
                              PmNpssSubjectMapper subjectMapper,
                              PmNpssSubjectMemberMapper memberMapper) {
        this.templateMapper = templateMapper;
        this.subjectMapper = subjectMapper;
        this.memberMapper = memberMapper;
    }

    /** 物化主体（供发起评价用）：主体权重/受益方 + 成员干系人 id。 */
    public record MaterializedSubject(Long subjectId, BigDecimal weight, boolean beneficiary,
                                      List<Long> memberStakeholderIds) {
    }

    // ===================== 租户级模板 =====================

    public List<SubjectTemplateVO> listTemplates() {
        return templateMapper.selectList(Wrappers.<PmNpssSubjectTemplate>lambdaQuery()
                        .orderByAsc(PmNpssSubjectTemplate::getSort))
                .stream()
                .map(t -> new SubjectTemplateVO(t.getId(), t.getName(), t.getWeight(),
                        isTrue(t.getBeneficiary()), t.getSort(), isTrue(t.getEnabled())))
                .toList();
    }

    /** 整组保存模板（replace-all）：先校验启用主体权重合计=100% 且受益方≥50%，再覆盖落库。 */
    @Transactional(rollbackFor = Exception.class)
    public void saveTemplates(List<SubjectTemplateDTO> items) {
        List<SubjectTemplateDTO> list = items == null ? List.of() : items;
        // 启用主体做 §4 硬校验
        List<SubjectWeight> enabled = list.stream()
                .filter(t -> !Boolean.FALSE.equals(t.enabled()))
                .map(t -> new SubjectWeight(t.weight(), Boolean.TRUE.equals(t.beneficiary())))
                .toList();
        SubjectWeightValidator.validate(enabled);

        templateMapper.delete(Wrappers.<PmNpssSubjectTemplate>lambdaQuery());
        int i = 0;
        for (SubjectTemplateDTO t : list) {
            PmNpssSubjectTemplate e = new PmNpssSubjectTemplate();
            e.setName(t.name());
            e.setWeight(t.weight());
            e.setBeneficiary(Boolean.TRUE.equals(t.beneficiary()) ? 1 : 0);
            e.setSort(t.sort() == null ? i : t.sort());
            e.setEnabled(Boolean.FALSE.equals(t.enabled()) ? 0 : 1);
            templateMapper.insert(e);
            i++;
        }
    }

    // ===================== 项目级评价主体 =====================

    /**
     * 项目评价主体：主体名称/权重/受益方一律取自租户启用模板（单一数据源，项目只读），
     * 成员（干系人）取自项目级配置；未配置成员的主体返回空成员，供前端按主体补充。
     */
    public List<ProjectSubjectVO> listProjectSubjects(Long projectId) {
        List<PmNpssSubjectTemplate> templates = enabledTemplates();
        List<PmNpssSubject> projectRows = projectSubjects(projectId);
        Map<Long, PmNpssSubject> rowByTemplate = projectRows.stream()
                .filter(s -> s.getTemplateId() != null)
                .collect(Collectors.toMap(PmNpssSubject::getTemplateId, s -> s, (a, b) -> a));
        Map<Long, List<Long>> membersBySubject = membersBySubject(projectRows);
        List<ProjectSubjectVO> out = new ArrayList<>();
        for (PmNpssSubjectTemplate t : templates) {
            PmNpssSubject row = rowByTemplate.get(t.getId());
            List<Long> members = row == null ? List.of()
                    : membersBySubject.getOrDefault(row.getId(), List.of());
            out.add(new ProjectSubjectVO(row == null ? null : row.getId(), projectId, t.getId(),
                    t.getName(), t.getWeight(), isTrue(t.getBeneficiary()), t.getSort(), members));
        }
        return out;
    }

    /**
     * 整组保存项目评价主体成员（replace-all）。主体集合/权重以租户启用模板为准，项目不可增删主体或改权重；
     * 仅校验每个启用主体≥1 成员、同一干系人不跨主体，再按模板物化落库（成员为项目级）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveProjectSubjects(Long projectId, List<ProjectSubjectDTO> items) {
        List<ProjectSubjectDTO> list = items == null ? List.of() : items;
        List<PmNpssSubjectTemplate> templates = enabledTemplates();
        if (templates.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "尚未配置租户级评价主体模板，请先在管理后台配置");
        }
        Map<Long, List<Long>> membersByTemplate = new LinkedHashMap<>();
        for (ProjectSubjectDTO s : list) {
            if (s.templateId() != null) {
                membersByTemplate.put(s.templateId(),
                        s.memberStakeholderIds() == null ? List.of() : s.memberStakeholderIds());
            }
        }
        // 校验：每个启用主体须≥1 成员；同一干系人只能归属一个主体（评分按 review+stakeholder 唯一）
        Set<Long> seen = new HashSet<>();
        for (PmNpssSubjectTemplate t : templates) {
            List<Long> members = membersByTemplate.get(t.getId());
            if (members == null || members.isEmpty()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "评价主体「" + t.getName() + "」至少需 1 名成员");
            }
            for (Long shId : members) {
                if (!seen.add(shId)) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "干系人不能同时属于多个评价主体: " + shId);
                }
            }
        }
        // 覆盖：删除项目旧主体及其成员
        List<PmNpssSubject> old = projectSubjects(projectId);
        if (!old.isEmpty()) {
            List<Long> oldIds = old.stream().map(PmNpssSubject::getId).toList();
            memberMapper.delete(Wrappers.<PmNpssSubjectMember>lambdaQuery()
                    .in(PmNpssSubjectMember::getSubjectId, oldIds));
            subjectMapper.delete(Wrappers.<PmNpssSubject>lambdaQuery()
                    .eq(PmNpssSubject::getProjectId, projectId));
        }
        // 物化：主体名称/权重/受益方以模板为准，仅成员为项目级
        for (PmNpssSubjectTemplate t : templates) {
            PmNpssSubject e = new PmNpssSubject();
            e.setProjectId(projectId);
            e.setTemplateId(t.getId());
            e.setName(t.getName());
            e.setWeight(t.getWeight());
            e.setBeneficiary(t.getBeneficiary());
            e.setSort(t.getSort());
            subjectMapper.insert(e);
            for (Long stakeholderId : membersByTemplate.get(t.getId())) {
                PmNpssSubjectMember m = new PmNpssSubjectMember();
                m.setSubjectId(e.getId());
                m.setStakeholderId(stakeholderId);
                memberMapper.insert(m);
            }
        }
    }

    /**
     * 供发起评价物化：已配置成员的项目主体 + 成员；权重/受益方以租户模板为准（单一数据源，
     * 模板缺失时回退项目快照）。未配置返回空（调用方回退个人口径）。
     */
    public List<MaterializedSubject> subjectsForReview(Long projectId) {
        List<PmNpssSubject> subjects = projectSubjects(projectId);
        if (subjects.isEmpty()) {
            return List.of();
        }
        Map<Long, PmNpssSubjectTemplate> templatesById = enabledTemplates().stream()
                .collect(Collectors.toMap(PmNpssSubjectTemplate::getId, t -> t, (a, b) -> a));
        Map<Long, List<Long>> membersBySubject = membersBySubject(subjects);
        return subjects.stream()
                .map(s -> {
                    PmNpssSubjectTemplate t = s.getTemplateId() == null ? null
                            : templatesById.get(s.getTemplateId());
                    BigDecimal weight = t != null ? t.getWeight() : s.getWeight();
                    boolean beneficiary = t != null ? isTrue(t.getBeneficiary()) : isTrue(s.getBeneficiary());
                    return new MaterializedSubject(s.getId(), weight, beneficiary,
                            membersBySubject.getOrDefault(s.getId(), List.of()));
                })
                .toList();
    }

    // ===================== 私有 =====================

    /** 租户启用模板（按 sort 升序）：项目级主体/权重的唯一数据源。 */
    private List<PmNpssSubjectTemplate> enabledTemplates() {
        return templateMapper.selectList(Wrappers.<PmNpssSubjectTemplate>lambdaQuery()
                        .orderByAsc(PmNpssSubjectTemplate::getSort))
                .stream().filter(t -> isTrue(t.getEnabled())).toList();
    }

    private List<PmNpssSubject> projectSubjects(Long projectId) {
        return subjectMapper.selectList(Wrappers.<PmNpssSubject>lambdaQuery()
                .eq(PmNpssSubject::getProjectId, projectId)
                .orderByAsc(PmNpssSubject::getSort));
    }

    private Map<Long, List<Long>> membersBySubject(List<PmNpssSubject> subjects) {
        List<Long> subjectIds = subjects.stream().map(PmNpssSubject::getId).toList();
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        return memberMapper.selectList(Wrappers.<PmNpssSubjectMember>lambdaQuery()
                        .in(PmNpssSubjectMember::getSubjectId, subjectIds))
                .stream()
                .collect(Collectors.groupingBy(PmNpssSubjectMember::getSubjectId,
                        Collectors.mapping(PmNpssSubjectMember::getStakeholderId,
                                Collectors.toCollection(ArrayList::new))));
    }

    private static boolean isTrue(Integer v) {
        return v != null && v == 1;
    }
}
