package com.mido.pm.org.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.org.dto.WecomConfigSaveDTO;
import com.mido.pm.org.dto.WecomConfigStatusVO;
import com.mido.pm.org.dto.WecomConfigVO;
import com.mido.pm.org.service.WecomConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 企业微信集成配置（租户侧可视化入口）。secret 入库加密、出参脱敏。 */
@RestController
@RequestMapping("/api/v1/wecom/config")
public class WecomConfigController {

    private final WecomConfigService configService;

    public WecomConfigController(WecomConfigService configService) {
        this.configService = configService;
    }

    /** 当前租户企微配置（脱敏）。 */
    @GetMapping
    public R<WecomConfigVO> get() {
        return R.ok(configService.get());
    }

    /** 各能力启用状态（供「企微同步」等入口联动）。 */
    @GetMapping("/status")
    public R<WecomConfigStatusVO> status() {
        return R.ok(configService.status());
    }

    /** 保存配置。 */
    @PreAuthorize("hasAuthority('org:user:create')")
    @PutMapping
    public R<Void> save(@RequestBody WecomConfigSaveDTO dto) {
        configService.save(dto);
        return R.ok();
    }
}
