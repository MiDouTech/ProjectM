package com.mido.pm.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.project.dto.PortfolioOverviewVO;
import com.mido.pm.project.dto.PortfolioSaveDTO;
import com.mido.pm.project.dto.PortfolioVO;
import com.mido.pm.project.dto.ProjectVO;
import com.mido.pm.project.entity.PmPortfolio;
import com.mido.pm.project.entity.PmPortfolioProject;
import com.mido.pm.project.mapper.PmPortfolioMapper;
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
    private final ProjectService projectService;

    public PortfolioService(PmPortfolioMapper portfolioMapper, PmPortfolioProjectMapper linkMapper,
                            ProjectService projectService) {
        this.portfolioMapper = portfolioMapper;
        this.linkMapper = linkMapper;
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

    public Long create(PortfolioSaveDTO dto) {
        PmPortfolio p = new PmPortfolio();
        p.setName(dto.name());
        p.setDescription(dto.description());
        p.setOwnerId(dto.ownerId());
        p.setStatus(dto.status() == null ? STATUS_ACTIVE : dto.status());
        portfolioMapper.insert(p);
        return p.getId();
    }

    public void update(Long id, PortfolioSaveDTO dto) {
        PmPortfolio p = requireExists(id);
        p.setName(dto.name());
        p.setDescription(dto.description());
        p.setOwnerId(dto.ownerId());
        if (dto.status() != null) {
            p.setStatus(dto.status());
        }
        portfolioMapper.updateById(p);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExists(id);
        linkMapper.delete(Wrappers.<PmPortfolioProject>lambdaQuery().eq(PmPortfolioProject::getPortfolioId, id));
        portfolioMapper.deleteById(id);
    }

    /** 向项目集挂接项目（去重，已存在的跳过）。 */
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
