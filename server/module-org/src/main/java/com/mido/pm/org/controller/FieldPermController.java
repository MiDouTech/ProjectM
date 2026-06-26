package com.mido.pm.org.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.common.security.FieldPermGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * 字段级权限（面向当前用户）：返回当前登录用户在某资源下的只读字段，供前端表单只读渲染。
 * 写入拦截的安全边界在 {@link FieldPermGuard}（服务层），本接口仅为 UX。
 */
@RestController
@RequestMapping("/api/v1/field-perms")
public class FieldPermController {

    private final FieldPermGuard fieldPermGuard;

    public FieldPermController(FieldPermGuard fieldPermGuard) {
        this.fieldPermGuard = fieldPermGuard;
    }

    /** 当前用户在 resource（task/project）下的只读字段键集合。 */
    @GetMapping("/view-only")
    public R<Set<String>> viewOnly(@RequestParam String resource) {
        return R.ok(fieldPermGuard.viewOnlyFields(resource));
    }
}
