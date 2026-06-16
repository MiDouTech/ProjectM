package com.mido.pm.org.controller;

import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.api.R;
import com.mido.pm.org.dto.AssignRolesDTO;
import com.mido.pm.org.dto.UserCreateDTO;
import com.mido.pm.org.dto.UserQueryDTO;
import com.mido.pm.org.dto.UserUpdateDTO;
import com.mido.pm.org.dto.UserVO;
import com.mido.pm.org.service.SysUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户 CRUD（含 job_level）+ 分配角色。 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final SysUserService userService;

    public UserController(SysUserService userService) {
        this.userService = userService;
    }

    /** 复杂查询用 POST /query（api-conventions §3）。 */
    @PostMapping("/query")
    public R<PageResult<UserVO>> query(@RequestBody UserQueryDTO query) {
        return R.ok(userService.page(query));
    }

    @GetMapping("/{id}")
    public R<UserVO> get(@PathVariable Long id) {
        return R.ok(userService.get(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody UserCreateDTO dto) {
        return R.ok(userService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        userService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesDTO dto) {
        userService.assignRoles(id, dto.roleIds());
        return R.ok();
    }
}
