package com.mido.pm.verify.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.verify.dto.ResultVerifySaveDTO;
import com.mido.pm.verify.dto.ResultVerifyVO;
import com.mido.pm.verify.service.ResultVerifyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结果验收（铁三角）：录入/查询 PMO 结论。结案闸门由 {@link ResultVerifyService} 经
 * ResultVerifyGate 在项目流转处强制，本控制器仅负责结论的录入与回显。
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/result-verify")
public class ResultVerifyController {

    private final ResultVerifyService resultVerifyService;

    public ResultVerifyController(ResultVerifyService resultVerifyService) {
        this.resultVerifyService = resultVerifyService;
    }

    /** 最新结果验收结论；无则返回 null。 */
    @GetMapping
    public R<ResultVerifyVO> latest(@PathVariable Long projectId) {
        return R.ok(resultVerifyService.latest(projectId));
    }

    /** 录入结果验收结论（pass/fail + 三角达标项 + 备注）。 */
    @PostMapping
    public R<ResultVerifyVO> save(@PathVariable Long projectId,
                                  @Valid @RequestBody ResultVerifySaveDTO dto) {
        return R.ok(resultVerifyService.save(projectId, dto));
    }
}
