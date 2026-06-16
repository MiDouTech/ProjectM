package com.mido.pm.project.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.project.dto.CreateFromTemplateDTO;
import com.mido.pm.project.dto.ProjectCreateDTO;
import com.mido.pm.project.dto.ProjectFromTemplateVO;
import com.mido.pm.project.dto.TemplateVO;
import com.mido.pm.project.entity.PmProjectTemplate;
import com.mido.pm.project.mapper.PmProjectTemplateMapper;
import com.mido.pm.project.template.ProjectSkeletonProvisioner;
import com.mido.pm.project.template.TemplateConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目模板服务：模板查询 + 按模板创建项目（建项目本体 → 解析 config → 供给骨架端口）。
 */
@Service
public class ProjectTemplateService {

    private final PmProjectTemplateMapper templateMapper;
    private final ProjectService projectService;
    private final ProjectSkeletonProvisioner skeletonProvisioner;
    private final ObjectMapper objectMapper;

    public ProjectTemplateService(PmProjectTemplateMapper templateMapper, ProjectService projectService,
                                  ProjectSkeletonProvisioner skeletonProvisioner, ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.projectService = projectService;
        this.skeletonProvisioner = skeletonProvisioner;
        this.objectMapper = objectMapper;
    }

    public List<TemplateVO> list(String category) {
        return templateMapper.selectList(Wrappers.<PmProjectTemplate>lambdaQuery()
                        .eq(StrUtil.isNotBlank(category), PmProjectTemplate::getCategory, category)
                        .orderByDesc(PmProjectTemplate::getIsBuiltin).orderByAsc(PmProjectTemplate::getId))
                .stream().map(this::toVO).toList();
    }

    public TemplateVO get(Long id) {
        return toVO(requireExists(id));
    }

    /** 按模板创建项目：建项目本体 + 同事务供给骨架（任务/干系人/审批延后由各域落地）。 */
    @Transactional(rollbackFor = Exception.class)
    public ProjectFromTemplateVO createFromTemplate(CreateFromTemplateDTO dto) {
        PmProjectTemplate template = requireExists(dto.templateId());
        TemplateConfig config = parseConfig(template.getConfig());

        String subCategory = dto.subCategory() != null ? dto.subCategory() : template.getSubCategory();
        Long projectId = projectService.create(new ProjectCreateDTO(
                dto.name(), template.getCategory(), subCategory,
                dto.leaderId(), dto.budget(), template.getId(),
                template.getDescription(), dto.startDate(), dto.endDate()));

        skeletonProvisioner.provision(projectId, config);
        return new ProjectFromTemplateVO(projectId, config);
    }

    private TemplateConfig parseConfig(String json) {
        if (StrUtil.isBlank(json)) {
            return new TemplateConfig(List.of(), List.of(), null, null);
        }
        try {
            return objectMapper.readValue(json, TemplateConfig.class);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "模板配置解析失败: " + e.getMessage());
        }
    }

    private PmProjectTemplate requireExists(Long id) {
        PmProjectTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目模板不存在");
        }
        return t;
    }

    private TemplateVO toVO(PmProjectTemplate t) {
        return new TemplateVO(t.getId(), t.getName(), t.getCategory(), t.getSubCategory(),
                t.getDescription(), t.getIsBuiltin());
    }
}
