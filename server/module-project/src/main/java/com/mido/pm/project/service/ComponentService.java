package com.mido.pm.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.project.dto.ComponentVO;
import com.mido.pm.project.dto.ProjectComponentSaveDTO;
import com.mido.pm.project.dto.ProjectComponentVO;
import com.mido.pm.project.entity.PmComponent;
import com.mido.pm.project.entity.PmProjectComponent;
import com.mido.pm.project.mapper.PmComponentMapper;
import com.mido.pm.project.mapper.PmProjectComponentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 组件服务：组件库(catalog) + 项目已安装组件。项目顶栏由已安装组件动态生成。
 * 项目无安装记录时前端回落默认全量 Tab；安装后按已装组件渲染与排序。
 */
@Service
public class ComponentService {

    private final PmComponentMapper componentMapper;
    private final PmProjectComponentMapper projectComponentMapper;

    public ComponentService(PmComponentMapper componentMapper, PmProjectComponentMapper projectComponentMapper) {
        this.componentMapper = componentMapper;
        this.projectComponentMapper = projectComponentMapper;
    }

    /** 组件库目录（按 sort 升序）。 */
    public List<ComponentVO> catalog() {
        return componentMapper.selectList(Wrappers.<PmComponent>lambdaQuery()
                        .orderByAsc(PmComponent::getSort).orderByAsc(PmComponent::getId))
                .stream().map(c -> new ComponentVO(c.getId(), c.getCode(), c.getName(), c.getIcon(),
                        c.getMultiInstance(), c.getBuiltin(), c.getSort())).toList();
    }

    /** 项目已安装组件（启用，按 sort 升序）。 */
    public List<ProjectComponentVO> listInstalled(Long projectId) {
        return projectComponentMapper.selectList(Wrappers.<PmProjectComponent>lambdaQuery()
                        .eq(PmProjectComponent::getProjectId, projectId)
                        .eq(PmProjectComponent::getEnabled, 1)
                        .orderByAsc(PmProjectComponent::getSort).orderByAsc(PmProjectComponent::getId))
                .stream().map(c -> new ProjectComponentVO(c.getId(), c.getComponentCode(), c.getName(), c.getSort()))
                .toList();
    }

    /** 整列表保存（先清后插，sort 取列表顺序）。空列表=回落默认 Tab。 */
    @Transactional(rollbackFor = Exception.class)
    public void saveInstalled(Long projectId, List<ProjectComponentSaveDTO> components) {
        projectComponentMapper.delete(Wrappers.<PmProjectComponent>lambdaQuery()
                .eq(PmProjectComponent::getProjectId, projectId));
        if (components == null) {
            return;
        }
        int sort = 0;
        for (ProjectComponentSaveDTO c : components) {
            PmProjectComponent pc = new PmProjectComponent();
            pc.setProjectId(projectId);
            pc.setComponentCode(c.componentCode());
            pc.setName(c.name());
            pc.setSort(c.sort() == null ? sort : c.sort());
            pc.setEnabled(1);
            projectComponentMapper.insert(pc);
            sort++;
        }
    }
}
