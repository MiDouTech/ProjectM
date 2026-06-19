package com.mido.pm.change.controller;

import com.mido.pm.change.dto.ChangeRequestVO;
import com.mido.pm.change.service.ChangeService;
import com.mido.pm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 变更中心：变更单台账查询（提交入口在各被改业务域，如 /goals/{id}/changes）。 */
@RestController
@RequestMapping("/api/v1/changes")
public class ChangeController {

    private final ChangeService changeService;

    public ChangeController(ChangeService changeService) {
        this.changeService = changeService;
    }

    /** 台账列表：按被改域/对象/状态可选过滤。 */
    @GetMapping
    public R<List<ChangeRequestVO>> list(@RequestParam(required = false) String bizType,
                                         @RequestParam(required = false) Long bizId,
                                         @RequestParam(required = false) String status) {
        return R.ok(changeService.list(bizType, bizId, status));
    }

    @GetMapping("/{id}")
    public R<ChangeRequestVO> get(@PathVariable Long id) {
        return R.ok(changeService.get(id));
    }
}
