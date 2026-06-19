package com.mido.pm.doc.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.doc.dto.PublicDocVO;
import com.mido.pm.doc.service.DocAclService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开分享只读访问（匿名，无需登录）。路径 /api/v1/public/** 在 SecurityConfig 放行。
 * 仅暴露 token → 标题+正文，不泄露任何内部 id/权限信息。
 */
@RestController
@RequestMapping("/api/v1/public/docs")
public class PublicDocController {

    private final DocAclService aclService;

    public PublicDocController(DocAclService aclService) {
        this.aclService = aclService;
    }

    @GetMapping("/{token}")
    public R<PublicDocVO> view(@PathVariable String token) {
        return R.ok(aclService.getByToken(token));
    }
}
