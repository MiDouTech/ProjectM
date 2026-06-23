package com.mido.pm.briefing.controller;

import com.mido.pm.briefing.dto.IssueCreateDTO;
import com.mido.pm.briefing.dto.IssueVO;
import com.mido.pm.briefing.service.BriefingIssueService;
import com.mido.pm.common.api.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 简报跟进问题：提出 / 我跟进的 / 改状态。 */
@RestController
@RequestMapping("/api/v1/briefing-issues")
public class BriefingIssueController {

    private final BriefingIssueService issueService;

    public BriefingIssueController(BriefingIssueService issueService) {
        this.issueService = issueService;
    }

    /** 跟进的问题：我提出的或我负责的（status 可空）。 */
    @GetMapping
    public R<List<IssueVO>> listMine(@RequestParam(required = false) String status) {
        return R.ok(issueService.listMine(status));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody IssueCreateDTO dto) {
        return R.ok(issueService.create(dto));
    }

    /** 改状态：{ status: open|following|closed }。 */
    @PatchMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        issueService.updateStatus(id, body.get("status"));
        return R.ok();
    }
}
