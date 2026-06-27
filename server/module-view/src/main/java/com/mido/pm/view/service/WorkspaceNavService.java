package com.mido.pm.view.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.view.domain.WorkspaceCatalog;
import com.mido.pm.view.domain.WorkspaceCatalog.ComponentDef;
import com.mido.pm.view.dto.NavItemSaveDTO;
import com.mido.pm.view.dto.NavNodeVO;
import com.mido.pm.view.entity.PmModuleNav;
import com.mido.pm.view.mapper.PmModuleNavMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 可配置工作区导航（ADR-0003 · L1/L2）：解析某一级模块的导航树（租户编排优先，空配置回落内置默认），
 * 及租户编排的整组保存。承载于 module-view（视图/工作区呈现域）。
 */
@Service
public class WorkspaceNavService {

    private final PmModuleNavMapper navMapper;

    public WorkspaceNavService(PmModuleNavMapper navMapper) {
        this.navMapper = navMapper;
    }

    /** 解析导航树：有编排按编排（仅启用项），无编排回落内置默认。 */
    public List<NavNodeVO> resolve(String module) {
        requireKnown(module);
        List<PmModuleNav> rows = navMapper.selectList(Wrappers.<PmModuleNav>lambdaQuery()
                .eq(PmModuleNav::getModule, module)
                .orderByAsc(PmModuleNav::getSort));
        if (rows.isEmpty()) {
            return defaultNav(module);
        }
        List<PmModuleNav> enabled = rows.stream()
                .filter(r -> !Integer.valueOf(0).equals(r.getEnabled())).toList();
        Map<String, List<PmModuleNav>> byParent = enabled.stream()
                .filter(r -> StringUtils.hasText(r.getParentCode()))
                .collect(Collectors.groupingBy(PmModuleNav::getParentCode));
        List<NavNodeVO> tree = new ArrayList<>();
        for (PmModuleNav r : enabled) {
            if (StringUtils.hasText(r.getParentCode())) {
                continue; // 子节点由父挂载
            }
            List<NavNodeVO> children = byParent.getOrDefault(r.getComponentCode(), List.of())
                    .stream().map(c -> toNode(module, c, List.of())).toList();
            tree.add(toNode(module, r, children));
        }
        return tree;
    }

    /** 某模块可选组件（catalog，编排器用）。 */
    public List<ComponentDef> catalog(String module) {
        requireKnown(module);
        return WorkspaceCatalog.catalog(module);
    }

    /** 原始编排（含停用项，按 sort），供编排器回显；无编排返回空表示"用默认"。 */
    public List<NavItemSaveDTO> rawConfig(String module) {
        requireKnown(module);
        return navMapper.selectList(Wrappers.<PmModuleNav>lambdaQuery()
                        .eq(PmModuleNav::getModule, module)
                        .orderByAsc(PmModuleNav::getSort))
                .stream()
                .map(r -> new NavItemSaveDTO(r.getComponentCode(), r.getParentCode(),
                        r.getDisplayName(), r.getIcon(), !Integer.valueOf(0).equals(r.getEnabled())))
                .toList();
    }

    /** 整组替换某模块的导航编排（replace-all，按列表顺序为 sort）。 */
    @Transactional(rollbackFor = Exception.class)
    public void saveNav(String module, List<NavItemSaveDTO> items) {
        requireKnown(module);
        navMapper.delete(Wrappers.<PmModuleNav>lambdaQuery().eq(PmModuleNav::getModule, module));
        if (items == null) {
            return;
        }
        int sort = 0;
        for (NavItemSaveDTO it : items) {
            if (WorkspaceCatalog.find(module, it.componentCode()) == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "未知组件: " + it.componentCode());
            }
            PmModuleNav row = new PmModuleNav();
            row.setModule(module);
            row.setComponentCode(it.componentCode());
            row.setParentCode(StringUtils.hasText(it.parentCode()) ? it.parentCode() : null);
            row.setDisplayName(it.displayName());
            row.setIcon(it.icon());
            row.setSort(sort++);
            row.setEnabled(Boolean.FALSE.equals(it.enabled()) ? 0 : 1);
            navMapper.insert(row);
        }
    }

    private List<NavNodeVO> defaultNav(String module) {
        return WorkspaceCatalog.catalog(module).stream()
                .map(d -> new NavNodeVO(d.code(), d.name(), d.icon(), d.route(), List.of()))
                .toList();
    }

    private NavNodeVO toNode(String module, PmModuleNav r, List<NavNodeVO> children) {
        ComponentDef def = WorkspaceCatalog.find(module, r.getComponentCode());
        String name = StringUtils.hasText(r.getDisplayName()) ? r.getDisplayName()
                : (def != null ? def.name() : r.getComponentCode());
        String icon = StringUtils.hasText(r.getIcon()) ? r.getIcon() : (def != null ? def.icon() : null);
        String route = def != null ? def.route() : null;
        return new NavNodeVO(r.getComponentCode(), name, icon, route, children);
    }

    private void requireKnown(String module) {
        if (!WorkspaceCatalog.knownModule(module)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未知模块: " + module);
        }
    }
}
