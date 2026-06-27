package com.mido.pm.project.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.project.dto.PortfolioOverviewVO;
import com.mido.pm.project.dto.PortfolioSaveDTO;
import com.mido.pm.project.dto.PortfolioVO;
import com.mido.pm.project.dto.ProjectVO;
import com.mido.pm.project.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 项目集 CRUD + 项目挂接 + 总览。 */
@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public R<List<PortfolioVO>> list() {
        return R.ok(portfolioService.list());
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody PortfolioSaveDTO dto) {
        return R.ok(portfolioService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PortfolioSaveDTO dto) {
        portfolioService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        portfolioService.delete(id);
        return R.ok();
    }

    /** 总览：项目集 + 当前用户可见项目（按数据范围）+ 状态计数。 */
    @GetMapping("/{id}/overview")
    public R<PortfolioOverviewVO> overview(@PathVariable Long id) {
        return R.ok(portfolioService.overview(id));
    }

    @PostMapping("/{id}/projects")
    public R<Void> addProjects(@PathVariable Long id, @RequestBody List<Long> projectIds) {
        portfolioService.addProjects(id, projectIds);
        return R.ok();
    }

    @DeleteMapping("/{id}/projects/{projectId}")
    public R<Void> removeProject(@PathVariable Long id, @PathVariable Long projectId) {
        portfolioService.removeProject(id, projectId);
        return R.ok();
    }

    /** 可加入的项目：创建人(owner)负责∪参与的项目（添加项目对话框用）。 */
    @GetMapping("/{id}/candidate-projects")
    public R<List<ProjectVO>> candidateProjects(@PathVariable Long id) {
        return R.ok(portfolioService.candidateProjects(id));
    }

    /** 项目集成员用户 id 列表。 */
    @GetMapping("/{id}/members")
    public R<List<Long>> members(@PathVariable Long id) {
        return R.ok(portfolioService.members(id));
    }

    /** 整组替换项目集成员（replace-all，创建人始终保留）。 */
    @PutMapping("/{id}/members")
    public R<Void> setMembers(@PathVariable Long id, @RequestBody List<Long> userIds) {
        portfolioService.setMembers(id, userIds);
        return R.ok();
    }
}
