package com.mido.pm.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.project.dto.ProjectTypeSaveDTO;
import com.mido.pm.project.dto.ProjectTypeVO;
import com.mido.pm.project.entity.PmProjectType;
import com.mido.pm.project.event.ProjectTypeEvents;
import com.mido.pm.project.mapper.PmProjectTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目类型服务：CRUD + 启停 + 排序。租户自配，取代硬编码枚举 S/I/O。
 * 任何写操作同事务写 Outbox 事件（CLAUDE.md 规则 3）。
 * 规则去硬编码（职级/NPSS/审批流读取本类型属性）与项目绑定在 P1 落地。
 */
@Service
public class ProjectTypeService {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_DISABLED = "disabled";

    private final PmProjectTypeMapper typeMapper;
    private final DomainEventPublisher eventPublisher;

    public ProjectTypeService(PmProjectTypeMapper typeMapper, DomainEventPublisher eventPublisher) {
        this.typeMapper = typeMapper;
        this.eventPublisher = eventPublisher;
    }

    /** 类型列表：onlyActive=true 仅返回启用态；按 sort 升序、id 升序。 */
    public List<ProjectTypeVO> list(boolean onlyActive) {
        return typeMapper.selectList(Wrappers.<PmProjectType>lambdaQuery()
                        .eq(onlyActive, PmProjectType::getStatus, STATUS_ACTIVE)
                        .orderByAsc(PmProjectType::getSort).orderByAsc(PmProjectType::getId))
                .stream().map(this::toVO).toList();
    }

    public ProjectTypeVO get(Long id) {
        return toVO(requireExists(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectTypeSaveDTO dto) {
        assertCodeUnique(dto.code(), null);
        PmProjectType t = new PmProjectType();
        t.setCode(dto.code());
        applyEditable(t, dto);
        t.setStatus(STATUS_ACTIVE);
        typeMapper.insert(t);

        eventPublisher.publish(ProjectTypeEvents.CREATED, payload(t.getId(), t.getCode()));
        return t.getId();
    }

    /** 更新：code 不可改（以路径 id 为准），其余字段整体覆盖。 */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProjectTypeSaveDTO dto) {
        PmProjectType t = requireExists(id);
        applyEditable(t, dto);
        typeMapper.updateById(t);

        eventPublisher.publish(ProjectTypeEvents.UPDATED, payload(t.getId(), t.getCode()));
    }

    /** 启用/停用：停用后该类型不再可用于新建项目（存量项目不受影响）。 */
    @Transactional(rollbackFor = Exception.class)
    public void setStatus(Long id, boolean active) {
        PmProjectType t = requireExists(id);
        t.setStatus(active ? STATUS_ACTIVE : STATUS_DISABLED);
        typeMapper.updateById(t);

        if (!active) {
            eventPublisher.publish(ProjectTypeEvents.DISABLED, payload(t.getId(), t.getCode()));
        } else {
            eventPublisher.publish(ProjectTypeEvents.UPDATED, payload(t.getId(), t.getCode()));
        }
    }

    /** 写入可编辑字段（code/status 不在此列）。 */
    private void applyEditable(PmProjectType t, ProjectTypeSaveDTO dto) {
        t.setName(dto.name());
        t.setParentCode(dto.parentCode());
        t.setColor(dto.color());
        t.setIcon(dto.icon());
        t.setSort(dto.sort() != null ? dto.sort() : 0);
        t.setMinJobLevel(dto.minJobLevel());
        t.setRequiresNpss(dto.requiresNpss() != null ? dto.requiresNpss() : 1);
        t.setDefaultFlowId(dto.defaultFlowId());
        t.setStakeholderTpl(dto.stakeholderTpl());
        t.setDescription(dto.description());
    }

    /** 校验 code 在租户内唯一（tenant 由拦截器注入，业务不手写）；excludeId 为更新时排除自身。 */
    private void assertCodeUnique(String code, Long excludeId) {
        Long exists = typeMapper.selectCount(Wrappers.<PmProjectType>lambdaQuery()
                .eq(PmProjectType::getCode, code)
                .ne(excludeId != null, PmProjectType::getId, excludeId));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "类型码已存在: " + code);
        }
    }

    private PmProjectType requireExists(Long id) {
        PmProjectType t = typeMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目类型不存在");
        }
        return t;
    }

    private Map<String, Object> payload(Long id, String code) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("typeId", id);
        m.put("code", code);
        return m;
    }

    private ProjectTypeVO toVO(PmProjectType t) {
        return new ProjectTypeVO(t.getId(), t.getCode(), t.getName(), t.getParentCode(),
                t.getColor(), t.getIcon(), t.getSort(), t.getMinJobLevel(), t.getRequiresNpss(),
                t.getDefaultFlowId(), t.getStakeholderTpl(), t.getStatus(), t.getDescription());
    }
}
