package com.mido.pm.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.project.dto.CreateFromTemplateDTO;
import com.mido.pm.project.dto.ProjectCreateDTO;
import com.mido.pm.project.dto.ProjectFromTemplateVO;
import com.mido.pm.project.entity.PmProjectTemplate;
import com.mido.pm.project.mapper.PmProjectTemplateMapper;
import com.mido.pm.project.template.ProjectSkeletonProvisioner;
import com.mido.pm.project.template.TemplateConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 按模板建项目单测：解析 config → 复用建项目 → 供给骨架端口；模板不存在拒绝。
 */
@ExtendWith(MockitoExtension.class)
class ProjectTemplateServiceTest {

    @Mock
    private PmProjectTemplateMapper templateMapper;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectSkeletonProvisioner provisioner;

    private ProjectTemplateService service;

    @BeforeEach
    void setUp() {
        service = new ProjectTemplateService(templateMapper, projectService, provisioner, new ObjectMapper());
    }

    @Test
    void createFromTemplateParsesAndProvisions() {
        PmProjectTemplate t = new PmProjectTemplate();
        t.setId(1L);
        t.setCategory("S");
        t.setConfig("{\"phases\":[{\"name\":\"立项\",\"tasks\":[\"填写立项申请\"]}],"
                + "\"stakeholders\":[{\"role\":\"sponsor\",\"weight\":30}],"
                + "\"approvalFlow\":\"S_STANDARD\",\"verifyMethod\":\"铁三角+NPSS\"}");
        when(templateMapper.selectById(1L)).thenReturn(t);
        when(projectService.create(any(ProjectCreateDTO.class))).thenReturn(100L);

        ProjectFromTemplateVO vo = service.createFromTemplate(
                new CreateFromTemplateDTO(1L, "项目A", 9L, null, null, null, null));

        assertEquals(100L, vo.projectId());
        assertEquals("S_STANDARD", vo.skeleton().approvalFlow());
        assertEquals(1, vo.skeleton().phases().size());
        verify(provisioner).provision(eq(100L), any(TemplateConfig.class));
    }

    @Test
    void templateNotFoundRejected() {
        when(templateMapper.selectById(9L)).thenReturn(null);
        assertThrows(BizException.class, () ->
                service.createFromTemplate(new CreateFromTemplateDTO(9L, "x", null, null, null, null, null)));
    }
}
