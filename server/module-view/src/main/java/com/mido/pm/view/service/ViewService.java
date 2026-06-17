package com.mido.pm.view.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.view.dto.ViewConfig;
import com.mido.pm.view.dto.ViewSaveDTO;
import com.mido.pm.view.dto.ViewVO;
import com.mido.pm.view.entity.PmView;
import com.mido.pm.view.mapper.PmViewMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 视图设计器服务：pm_view 的命名视图 CRUD（个人/项目级）。视图为个人/项目偏好配置，非业务事实，写操作不发领域事件。
 * config 仅承载查询配置（{@link ViewConfig}，结构锁定）；字段/算子白名单由查询侧 translator 强制。
 */
@Service
public class ViewService {

    public static final String SCOPE_PERSONAL = "personal";
    public static final String SCOPE_PROJECT = "project";
    private static final Set<String> SCOPES = Set.of(SCOPE_PERSONAL, SCOPE_PROJECT);
    private static final Set<String> TYPES = Set.of("kanban", "list", "table", "gantt", "calendar");

    private final PmViewMapper viewMapper;
    private final ObjectMapper objectMapper;

    public ViewService(PmViewMapper viewMapper, ObjectMapper objectMapper) {
        this.viewMapper = viewMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ViewSaveDTO dto) {
        validate(dto);
        PmView v = new PmView();
        v.setScope(dto.scope());
        v.setType(dto.type());
        v.setName(dto.name());
        v.setProjectId(SCOPE_PROJECT.equals(dto.scope()) ? dto.projectId() : null);
        v.setOwnerId(UserContext.currentUserId());
        v.setConfig(writeConfig(dto.config()));
        viewMapper.insert(v);
        return v.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ViewSaveDTO dto) {
        validate(dto);
        PmView v = requireOwned(id);
        v.setName(dto.name());
        v.setType(dto.type());
        v.setConfig(writeConfig(dto.config()));
        viewMapper.updateById(v);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireOwned(id);
        viewMapper.deleteById(id);
    }

    /** 当前项目可见视图：我的个人视图 ∪ 该项目的项目级视图。 */
    public List<ViewVO> listVisible(Long projectId) {
        Long me = UserContext.currentUserId();
        return viewMapper.selectList(Wrappers.<PmView>lambdaQuery()
                        .in(PmView::getScope, SCOPE_PERSONAL, SCOPE_PROJECT)
                        .and(w -> w
                                .eq(PmView::getScope, SCOPE_PERSONAL).eq(PmView::getOwnerId, me)
                                .or(o -> o.eq(PmView::getScope, SCOPE_PROJECT).eq(PmView::getProjectId, projectId)))
                        .orderByDesc(PmView::getId))
                .stream().map(this::toVO).toList();
    }

    public ViewVO get(Long id) {
        return toVO(requireExists(id));
    }

    /** 供查询侧按 viewId 解析配置。 */
    public ViewConfig getConfig(Long id) {
        return readConfig(requireExists(id).getConfig());
    }

    // ===== 内部 =====

    private void validate(ViewSaveDTO dto) {
        if (!SCOPES.contains(dto.scope())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法视图范围: " + dto.scope());
        }
        if (!TYPES.contains(dto.type())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法视图类型: " + dto.type());
        }
        if (SCOPE_PROJECT.equals(dto.scope()) && dto.projectId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "项目级视图须指定 projectId");
        }
        ViewConfig c = dto.config();
        if (c.expandLevel() != null && (c.expandLevel() < 1 || c.expandLevel() > 5)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "expandLevel 须在 1-5");
        }
        if (c.filters() != null && c.filters().logic() != null
                && !Set.of("and", "or").contains(c.filters().logic())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "filters.logic 须为 and/or");
        }
    }

    private PmView requireExists(Long id) {
        PmView v = viewMapper.selectById(id);
        if (v == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "视图不存在");
        }
        return v;
    }

    private PmView requireOwned(Long id) {
        PmView v = requireExists(id);
        if (v.getOwnerId() != null && !v.getOwnerId().equals(UserContext.currentUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能修改本人创建的视图");
        }
        return v;
    }

    private String writeConfig(ViewConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "视图配置序列化失败: " + e.getMessage());
        }
    }

    private ViewConfig readConfig(String json) {
        if (json == null || json.isBlank()) {
            return new ViewConfig(null, List.of(), 1, null, List.of());
        }
        try {
            return objectMapper.readValue(json, ViewConfig.class);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "视图配置解析失败: " + e.getMessage());
        }
    }

    private ViewVO toVO(PmView v) {
        return new ViewVO(v.getId(), v.getName(), v.getScope(), v.getType(),
                v.getProjectId(), v.getOwnerId(), readConfig(v.getConfig()));
    }
}
