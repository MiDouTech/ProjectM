package com.mido.pm.org.controller;

import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.api.R;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.org.dto.AssignRolesDTO;
import com.mido.pm.org.dto.ChangePasswordDTO;
import com.mido.pm.org.dto.ResetPasswordDTO;
import com.mido.pm.org.dto.UserCreateDTO;
import com.mido.pm.org.dto.UserQueryDTO;
import com.mido.pm.org.dto.UserUpdateDTO;
import com.mido.pm.org.dto.UserVO;
import com.mido.pm.org.service.SysUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /** 当前登录用户信息（替代前端从 JWT 自解析，提供姓名/头像/部门/职级）。 */
    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(userService.get(UserContext.currentUserId()));
    }

    /** 自助修改密码：校验原密码后设置新密码（首登默认密码须改）。 */
    @PutMapping("/me/password")
    public R<Void> changeMyPassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(UserContext.currentUserId(), dto.oldPassword(), dto.newPassword());
        return R.ok();
    }

    /** 复杂查询用 POST /query（api-conventions §3）。 */
    @PreAuthorize("hasAuthority('org:user:query')")
    @PostMapping("/query")
    public R<PageResult<UserVO>> query(@RequestBody UserQueryDTO query) {
        return R.ok(userService.page(query));
    }

    @GetMapping("/{id}")
    public R<UserVO> get(@PathVariable Long id) {
        return R.ok(userService.get(id));
    }

    @PreAuthorize("hasAuthority('org:user:create')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody UserCreateDTO dto) {
        return R.ok(userService.create(dto));
    }

    @PreAuthorize("hasAuthority('org:user:create')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        userService.update(id, dto);
        return R.ok();
    }

    @PreAuthorize("hasAuthority('org:user:create')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    @PreAuthorize("hasAuthority('org:user:create')")
    @PutMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesDTO dto) {
        userService.assignRoles(id, dto.roleIds());
        return R.ok();
    }

    /** 管理员重置指定用户密码（与 update/delete 同级保护，操作留审计）。 */
    @PreAuthorize("hasAuthority('org:user:create')")
    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(id, dto.newPassword());
        return R.ok();
    }
}
