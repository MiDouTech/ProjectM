package com.mido.pm.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.Audited;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.project.dto.PortfolioOverviewVO;
import com.mido.pm.project.dto.PortfolioSaveDTO;
import com.mido.pm.project.dto.PortfolioVO;
import com.mido.pm.project.dto.ProjectVO;
import com.mido.pm.project.entity.PmPortfolio;
import com.mido.pm.project.entity.PmPortfolioMember;
import com.mido.pm.project.entity.PmPortfolioProject;
import com.mido.pm.project.mapper.PmPortfolioMapper;
import com.mido.pm.project.mapper.PmPortfolioMemberMapper;
import com.mido.pm.project.mapper.PmPortfolioProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目集服务：CRUD + 项目挂接 + 总览。
 * 总览的项目列表经 {@link ProjectService#visibleByIds} 按数据范围过滤，
 * 管理层(ALL)看全公司、部门成员看权限内，天然实现全局视图与局部视图隔离。
 */
@Service
public class PortfolioService {

    private static final String STATUS_ACTIVE = "active";

    private final PmPortfolioMapper portfolioMapper;
    private final PmPortfolioProjectMapper linkMapper;
    private final PmPortfolioMemberMapper memberMapper;
    private final ProjectService projectService;

    public PortfolioService(PmPortfolioMapper portfolioMapper, PmPortfolioProjectMapper linkMapper,
                            PmPortfolioMemberMapper memberMapper, ProjectService projectService) {
        this.portfolioMapper = portfolioMapper;
        this.linkMapper = linkMapper;
        this.memberMapper = memberMapper;
        this.projectService = projectService;
    }

    public List<PortfolioVO> list() {
        List<PmPortfolio> portfolios = portfolioMapper.selectList(Wrappers.<PmPortfolio>lambdaQuery()
                .orderByDesc(PmPortfolio::getId));
        // 一次取全部关联，内存按 portfolio 分组计数，避免逐条 selectCount 的 N+1
        Map<Long, Long> counts = linkMapper.selectList(Wrappers.<PmPortfolioProject>lambdaQuery())
                .stream().collect(Collectors.groupingBy(PmPortfolioProject::getPortfolioId, Collectors.counting()));
        return portfolios.stream()
                .map(p -> toVO(p, counts.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    @Audited(module = AuditActions.MODULE_PROJECT, action = AuditActions.CREATED, target = AuditActions.TARGET_PORTFOLIO)
    @Transactional(rollbackFor = Exception.class)
    public Long create(PortfolioSaveDTO dto) {
        // 创建人默认取当前登录用户（前端传 ownerId 时以其为准，便于代建）
        Long ownerId = dto.ownerId() != null ? dto.ownerId() : UserContext.currentUserId();
        PmPortfolio p = new PmPortfolio();
        p.setName(dto.name());
        p.setDescription(dto.description());
        p.setOwnerId(ownerId);
        p.setStatus(dto.status() == null ? STATUS_ACTIVE : dto.status());
        portfolioMapper.insert(p);
        // 创建人默认即首个成员；附加传入成员去重加入
        addMembersInternal(p.getId(), mergeOwnerInto(ownerId, dto.memberIds()));
        return p.getId();
    }

    @Audited(module = AuditActions.MODULE_PROJECT, action = AuditActions.UPDATED, target = AuditActions.TARGET_PORTFOLIO)
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PortfolioSaveDTO dto) {
        PmPortfolio p = requireExists(id);
        p.setName(dto.name());
        p.setDescription(dto.description());
        p.setOwnerId(dto.ownerId());
        if (dto.status() != null) {
            p.setStatus(dto.status());
        }
        portfolioMapper.updateById(p);
        // memberIds 非 null 才整组替换；始终保证创建人(owner)在成员内
        if (dto.memberIds() != null) {
            setMembers(id, mergeOwnerInto(p.getOwnerId(), dto.memberIds()));
        }
    }

    /** 项目集成员用户 id 列表。 */
    public List<Long> members(Long portfolioId) {
        requireExists(portfolioId);
        return memberMapper.selectList(Wrappers.<PmPortfolioMember>lambdaQuery()
                        .eq(PmPortfolioMember::getPortfolioId, portfolioId))
                .stream().map(PmPortfolioMember::getUserId).toList();
    }

    /** 整组替换成员（replace-all），创建人始终保留。 */
    @Transactional(rollbackFor = Exception.class)
    public void setMembers(Long portfolioId, List<Long> userIds) {
        PmPortfolio p = requireExists(portfolioId);
        memberMapper.delete(Wrappers.<PmPortfolioMember>lambdaQuery()
                .eq(PmPortfolioMember::getPortfolioId, portfolioId));
        addMembersInternal(portfolioId, mergeOwnerInto(p.getOwnerId(), userIds));
    }

    /** 项目集「可加入的项目」：创建人(owner)负责∪参与的项目（与你确认的口径一致）。 */
    public List<ProjectVO> candidateProjects(Long portfolioId) {
        PmPortfolio p = requireExists(portfolioId);
        return projectService.projectsLedOrMemberOf(p.getOwnerId());
    }

    private void addMembersInternal(Long portfolioId, List<Long> userIds) {
        List<Long> existing = memberMapper.selectList(Wrappers.<PmPortfolioMember>lambdaQuery()
                        .eq(PmPortfolioMember::getPortfolioId, portfolioId))
                .stream().map(PmPortfolioMember::getUserId).toList();
        for (Long uid : userIds) {
            if (uid == null || existing.contains(uid)) {
                continue;
            }
            PmPortfolioMember m = new PmPortfolioMember();
            m.setPortfolioId(portfolioId);
            m.setUserId(uid);
            memberMapper.insert(m);
        }
    }

    /** 合并：确保创建人始终在成员集合内，去重。 */
    private List<Long> mergeOwnerInto(Long ownerId, List<Long> userIds) {
        java.util.LinkedHashSet<Long> set = new java.util.LinkedHashSet<>();
        if (ownerId != null) {
            set.add(ownerId);
        }
        if (userIds != null) {
            userIds.stream().filter(java.util.Objects::nonNull).forEach(set::add);
        }
        return new java.util.ArrayList<>(set);
    }

    @Audited(module = AuditActions.MODULE_PROJECT, action = AuditActions.DELETED, target = AuditActions.TARGET_PORTFOLIO)
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExists(id);
        linkMapper.delete(Wrappers.<PmPortfolioProject>lambdaQuery().eq(PmPortfolioProject::getPortfolioId, id));
        memberMapper.delete(Wrappers.<PmPortfolioMember>lambdaQuery().eq(PmPortfolioMember::getPortfolioId, id));
        portfolioMapper.deleteById(id);
    }

    /** 向项目集挂接项目（去重，已存在的跳过）。 */
    @Audited(module = AuditActions.MODULE_PROJECT, action = AuditActions.UPDATED, target = AuditActions.TARGET_PORTFOLIO)
    @Transactional(rollbackFor = Exception.class)
    public void addProjects(Long portfolioId, List<Long> projectIds) {
        requireExists(portfolioId);
        if (projectIds == null) {
            return;
        }
        List<Long> existing = linkedProjectIds(portfolioId);
        for (Long pid : projectIds) {
            if (pid == null || existing.contains(pid)) {
                continue;
            }
            PmPortfolioProject link = new PmPortfolioProject();
            link.setPortfolioId(portfolioId);
            link.setProjectId(pid);
            linkMapper.insert(link);
        }
    }

    @Audited(module = AuditActions.MODULE_PROJECT, action = AuditActions.UPDATED, target = AuditActions.TARGET_PORTFOLIO)
    public void removeProject(Long portfolioId, Long projectId) {
        requireExists(portfolioId);
        linkMapper.delete(Wrappers.<PmPortfolioProject>lambdaQuery()
                .eq(PmPortfolioProject::getPortfolioId, portfolioId)
                .eq(PmPortfolioProject::getProjectId, projectId));
    }

    /** 总览：项目集 + 当前用户可见项目（按数据范围过滤）+ 状态计数。 */
    public PortfolioOverviewVO overview(Long portfolioId) {
        PmPortfolio p = requireExists(portfolioId);
        List<Long> linkedIds = linkedProjectIds(portfolioId);
        List<ProjectVO> projects = projectService.visibleByIds(linkedIds);
        Map<String, Long> statusCount = projects.stream()
                .collect(Collectors.groupingBy(v -> v.status() == null ? "unknown" : v.status(),
                        Collectors.counting()));
        return new PortfolioOverviewVO(toVO(p, linkedIds.size()), projects, statusCount);
    }

    private List<Long> linkedProjectIds(Long portfolioId) {
        return linkMapper.selectList(Wrappers.<PmPortfolioProject>lambdaQuery()
                        .eq(PmPortfolioProject::getPortfolioId, portfolioId))
                .stream().map(PmPortfolioProject::getProjectId).toList();
    }

    private PmPortfolio requireExists(Long id) {
        PmPortfolio p = portfolioMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目集不存在");
        }
        return p;
    }

    private PortfolioVO toVO(PmPortfolio p, long projectCount) {
        return new PortfolioVO(p.getId(), p.getName(), p.getDescription(), p.getOwnerId(),
                p.getStatus(), projectCount);
    }
}
