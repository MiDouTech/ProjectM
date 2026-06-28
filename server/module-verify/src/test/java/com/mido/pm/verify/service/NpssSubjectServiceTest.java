package com.mido.pm.verify.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.verify.dto.ProjectSubjectDTO;
import com.mido.pm.verify.dto.SubjectTemplateDTO;
import com.mido.pm.verify.entity.PmNpssSubject;
import com.mido.pm.verify.entity.PmNpssSubjectMember;
import com.mido.pm.verify.entity.PmNpssSubjectTemplate;
import com.mido.pm.verify.mapper.PmNpssSubjectMapper;
import com.mido.pm.verify.mapper.PmNpssSubjectMemberMapper;
import com.mido.pm.verify.mapper.PmNpssSubjectTemplateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NPSS 评价方式配置校验（npss-rule §3A/§4 主体口径）：权重合计=100%、受益方≥50%、每主体≥1成员、干系人不跨主体。
 */
@ExtendWith(MockitoExtension.class)
class NpssSubjectServiceTest {

    @Mock private PmNpssSubjectTemplateMapper templateMapper;
    @Mock private PmNpssSubjectMapper subjectMapper;
    @Mock private PmNpssSubjectMemberMapper memberMapper;
    @InjectMocks private NpssSubjectService service;

    /** 项目级仅提交「模板主体 → 成员」；主体名/权重以租户模板为准。 */
    private static ProjectSubjectDTO psm(Long templateId, Long... members) {
        return new ProjectSubjectDTO(templateId, null, List.of(members));
    }

    /** 启用的租户模板主体。 */
    private static PmNpssSubjectTemplate tpl(Long id, String name, String weight, boolean beneficiary) {
        PmNpssSubjectTemplate t = new PmNpssSubjectTemplate();
        t.setId(id);
        t.setName(name);
        t.setWeight(new BigDecimal(weight));
        t.setBeneficiary(beneficiary ? 1 : 0);
        t.setEnabled(1);
        t.setSort(0);
        return t;
    }

    @Test
    void projectSubjectsRejectWhenNoTemplate() {
        when(templateMapper.selectList(any())).thenReturn(List.of()); // 租户未配置模板
        assertThrows(BizException.class, () -> service.saveProjectSubjects(100L, List.of(psm(10L, 1L))));
    }

    @Test
    void projectSubjectsRejectEmptyMember() {
        when(templateMapper.selectList(any())).thenReturn(List.of(
                tpl(10L, "受益方", "60", true), tpl(20L, "其他", "40", false)));
        // 主体 20 未配成员
        assertThrows(BizException.class, () -> service.saveProjectSubjects(100L, List.of(psm(10L, 1L))));
    }

    @Test
    void projectSubjectsRejectStakeholderInTwoSubjects() {
        when(templateMapper.selectList(any())).thenReturn(List.of(
                tpl(10L, "受益方", "60", true), tpl(20L, "其他", "40", false)));
        assertThrows(BizException.class, () -> service.saveProjectSubjects(100L, List.of(
                psm(10L, 1L), psm(20L, 1L))));
    }

    @Test
    void projectSubjectsSaveHappyPath() {
        when(templateMapper.selectList(any())).thenReturn(List.of(
                tpl(10L, "受益方", "60", true), tpl(20L, "其他", "40", false)));
        when(subjectMapper.selectList(any())).thenReturn(List.of()); // 项目原无主体
        service.saveProjectSubjects(100L, List.of(psm(10L, 1L, 2L), psm(20L, 3L)));
        verify(subjectMapper, Mockito.times(2)).insert(any(PmNpssSubject.class));
        verify(memberMapper, Mockito.times(3)).insert(any(PmNpssSubjectMember.class));
    }

    @Test
    void templatesRejectEnabledWeightsNot100() {
        assertThrows(BizException.class, () -> service.saveTemplates(List.of(
                new SubjectTemplateDTO(null, "受益方", new BigDecimal("60"), true, 0, true),
                new SubjectTemplateDTO(null, "其他", new BigDecimal("30"), false, 1, true))));
    }

    @Test
    void templatesSaveHappyPath() {
        service.saveTemplates(List.of(
                new SubjectTemplateDTO(null, "受益方", new BigDecimal("60"), true, 0, true),
                new SubjectTemplateDTO(null, "其他", new BigDecimal("40"), false, 1, true)));
        verify(templateMapper, Mockito.times(2)).insert(any(PmNpssSubjectTemplate.class));
    }
}
