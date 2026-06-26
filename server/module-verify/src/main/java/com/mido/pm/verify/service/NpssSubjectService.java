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
import java.util.List;
import java.util.Map;
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

    /** 项目评价主体：已配置则返回（含成员）；未配置则按启用模板派生空成员草稿，供前端预填。 */
    public List<ProjectSubjectVO> listProjectSubjects(Long projectId) {
        List<PmNpssSubject> subjects = projectSubjects(projectId);
        if (subjects.isEmpty()) {
            return listTemplates().stream().filter(SubjectTemplateVO::enabled)
                    .map(t -> new ProjectSubjectVO(null, projectId, t.name(), t.weight(),
                            t.beneficiary(), t.sort(), List.of()))
                    .toList();
        }
        Map<Long, List<Long>> membersBySubject = membersBySubject(subjects);
        return subjects.stream()
                .map(s -> new ProjectSubjectVO(s.getId(), s.getProjectId(), s.getName(), s.getWeight(),
                        isTrue(s.getBeneficiary()), s.getSort(),
                        membersBySubject.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    /** 整组保存项目评价主体（replace-all）：校验权重(§4 主体口径) + 每个主体≥1 成员，再覆盖落库。 */
    @Transactional(rollbackFor = Exception.class)
    public void saveProjectSubjects(Long projectId, List<ProjectSubjectDTO> items) {
        List<ProjectSubjectDTO> list = items == null ? List.of() : items;
        if (list.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "评价主体不能为空");
        }
        SubjectWeightValidator.validate(list.stream()
                .map(s -> new SubjectWeight(s.weight(), Boolean.TRUE.equals(s.beneficiary())))
                .toList());
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (ProjectSubjectDTO s : list) {
            if (s.memberStakeholderIds() == null || s.memberStakeholderIds().isEmpty()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "评价主体「" + s.name() + "」至少需 1 名成员");
            }
            for (Long shId : s.memberStakeholderIds()) {
                if (!seen.add(shId)) {
                    // 同一干系人只能归属一个主体（评分按 review+stakeholder 唯一，避免重复评分行）
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
        int i = 0;
        for (ProjectSubjectDTO s : list) {
            PmNpssSubject e = new PmNpssSubject();
            e.setProjectId(projectId);
            // replace-all 覆盖落库，旧主体 id 失效；模板来源非关键信息，留空
            e.setName(s.name());
            e.setWeight(s.weight());
            e.setBeneficiary(Boolean.TRUE.equals(s.beneficiary()) ? 1 : 0);
            e.setSort(s.sort() == null ? i : s.sort());
            subjectMapper.insert(e);
            for (Long stakeholderId : s.memberStakeholderIds()) {
                PmNpssSubjectMember m = new PmNpssSubjectMember();
                m.setSubjectId(e.getId());
                m.setStakeholderId(stakeholderId);
                memberMapper.insert(m);
            }
            i++;
        }
    }

    /** 供发起评价物化：已配置的项目主体 + 成员；未配置返回空（调用方回退个人口径）。 */
    public List<MaterializedSubject> subjectsForReview(Long projectId) {
        List<PmNpssSubject> subjects = projectSubjects(projectId);
        if (subjects.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Long>> membersBySubject = membersBySubject(subjects);
        return subjects.stream()
                .map(s -> new MaterializedSubject(s.getId(), s.getWeight(), isTrue(s.getBeneficiary()),
                        membersBySubject.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    // ===================== 私有 =====================

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
