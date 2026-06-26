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

    private static ProjectSubjectDTO ps(String name, String weight, boolean beneficiary, Long... members) {
        return new ProjectSubjectDTO(null, name, new BigDecimal(weight), beneficiary, null, List.of(members));
    }

    @Test
    void projectSubjectsRejectWeightsNot100() {
        assertThrows(BizException.class, () -> service.saveProjectSubjects(100L, List.of(
                ps("受益方", "40", true, 1L), ps("其他", "40", false, 2L))));
    }

    @Test
    void projectSubjectsRejectBeneficiaryLt50() {
        assertThrows(BizException.class, () -> service.saveProjectSubjects(100L, List.of(
                ps("受益方", "40", true, 1L), ps("其他", "60", false, 2L))));
    }

    @Test
    void projectSubjectsRejectEmptyMember() {
        assertThrows(BizException.class, () -> service.saveProjectSubjects(100L, List.of(
                ps("受益方", "60", true), ps("其他", "40", false, 2L))));
    }

    @Test
    void projectSubjectsRejectStakeholderInTwoSubjects() {
        assertThrows(BizException.class, () -> service.saveProjectSubjects(100L, List.of(
                ps("受益方", "60", true, 1L), ps("其他", "40", false, 1L))));
    }

    @Test
    void projectSubjectsSaveHappyPath() {
        when(subjectMapper.selectList(any())).thenReturn(List.of()); // 项目原无主体
        service.saveProjectSubjects(100L, List.of(
                ps("受益方", "60", true, 1L, 2L), ps("其他", "40", false, 3L)));
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
