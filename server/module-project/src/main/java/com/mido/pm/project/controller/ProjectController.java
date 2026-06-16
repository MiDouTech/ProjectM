package com.mido.pm.project.controller;

import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.api.R;
import com.mido.pm.project.dto.ProjectCreateDTO;
import com.mido.pm.project.dto.ProjectMemberCreateDTO;
import com.mido.pm.project.dto.ProjectMemberVO;
import com.mido.pm.project.dto.ProjectQueryDTO;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.dto.ProjectUpdateDTO;
import com.mido.pm.project.dto.ProjectVO;
import com.mido.pm.project.service.ProjectMemberService;
import com.mido.pm.project.service.ProjectService;
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

/** 项目 CRUD + 生命周期流转 + 成员管理。 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMemberService memberService;

    public ProjectController(ProjectService projectService, ProjectMemberService memberService) {
        this.projectService = projectService;
        this.memberService = memberService;
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody ProjectCreateDTO dto) {
        return R.ok(projectService.create(dto));
    }

    @GetMapping("/{id}")
    public R<ProjectVO> get(@PathVariable Long id) {
        return R.ok(projectService.get(id));
    }

    @PostMapping("/query")
    public R<PageResult<ProjectVO>> query(@RequestBody ProjectQueryDTO query) {
        return R.ok(projectService.page(query));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateDTO dto) {
        projectService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return R.ok();
    }

    /** 生命周期状态流转（合法性 + 职级/审批结果 guard）。 */
    @PostMapping("/{id}/transition")
    public R<Void> transition(@PathVariable Long id, @Valid @RequestBody ProjectTransitionDTO dto) {
        projectService.transition(id, dto);
        return R.ok();
    }

    @PostMapping("/{id}/members")
    public R<Long> addMember(@PathVariable Long id, @Valid @RequestBody ProjectMemberCreateDTO dto) {
        return R.ok(memberService.add(id, dto));
    }

    @GetMapping("/{id}/members")
    public R<List<ProjectMemberVO>> listMembers(@PathVariable Long id) {
        return R.ok(memberService.list(id));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public R<Void> removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        memberService.remove(id, memberId);
        return R.ok();
    }
}
