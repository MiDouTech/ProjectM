package com.mido.pm.org.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.DataScope;
import com.mido.pm.org.dto.DataScopeSettingDTO;
import com.mido.pm.org.dto.RoleCreateDTO;
import com.mido.pm.org.dto.RoleUpdateDTO;
import com.mido.pm.org.dto.RoleVO;
import com.mido.pm.org.entity.SysRole;
import com.mido.pm.org.entity.SysRoleDataScope;
import com.mido.pm.org.entity.SysRolePerm;
import com.mido.pm.org.mapper.SysRoleDataScopeMapper;
import com.mido.pm.org.mapper.SysRoleMapper;
import com.mido.pm.org.mapper.SysRolePermMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务：CRUD + 配权限码 + 配数据范围。
 */
@Service
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermMapper rolePermMapper;
    private final SysRoleDataScopeMapper roleDataScopeMapper;

    public SysRoleService(SysRoleMapper roleMapper, SysRolePermMapper rolePermMapper,
                          SysRoleDataScopeMapper roleDataScopeMapper) {
        this.roleMapper = roleMapper;
        this.rolePermMapper = rolePermMapper;
        this.roleDataScopeMapper = roleDataScopeMapper;
    }

    public List<RoleVO> list() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByDesc(SysRole::getId))
                .stream().map(this::toVO).toList();
    }

    public RoleVO get(Long id) {
        return toVO(requireExists(id));
    }

    public Long create(RoleCreateDTO dto) {
        Long exists = roleMapper.selectCount(
                Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, dto.code()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setName(dto.name());
        role.setCode(dto.code());
        roleMapper.insert(role);
        return role.getId();
    }

    public void update(Long id, RoleUpdateDTO dto) {
        SysRole role = requireExists(id);
        role.setName(dto.name());
        role.setCode(dto.code());
        roleMapper.updateById(role);
    }

    public void delete(Long id) {
        requireExists(id);
        roleMapper.deleteById(id);
    }

    // ===== 权限码 =====

    public List<String> getPerms(Long roleId) {
        requireExists(roleId);
        return rolePermMapper.selectList(
                        Wrappers.<SysRolePerm>lambdaQuery().eq(SysRolePerm::getRoleId, roleId))
                .stream().map(SysRolePerm::getPermCode).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void savePerms(Long roleId, List<String> permCodes) {
        requireExists(roleId);
        rolePermMapper.delete(Wrappers.<SysRolePerm>lambdaQuery().eq(SysRolePerm::getRoleId, roleId));
        if (permCodes == null) {
            return;
        }
        for (String code : permCodes) {
            SysRolePerm rp = new SysRolePerm();
            rp.setRoleId(roleId);
            rp.setPermCode(code);
            rolePermMapper.insert(rp);
        }
    }

    // ===== 数据范围 =====

    public List<DataScopeSettingDTO> getDataScopes(Long roleId) {
        requireExists(roleId);
        return roleDataScopeMapper.selectList(
                        Wrappers.<SysRoleDataScope>lambdaQuery().eq(SysRoleDataScope::getRoleId, roleId))
                .stream().map(s -> new DataScopeSettingDTO(s.getResource(), s.getScope())).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveDataScopes(Long roleId, List<DataScopeSettingDTO> settings) {
        requireExists(roleId);
        if (settings != null) {
            for (DataScopeSettingDTO s : settings) {
                if (DataScope.fromCode(s.scope(), null) == null) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "非法数据范围: " + s.scope());
                }
            }
        }
        roleDataScopeMapper.delete(
                Wrappers.<SysRoleDataScope>lambdaQuery().eq(SysRoleDataScope::getRoleId, roleId));
        if (settings == null) {
            return;
        }
        for (DataScopeSettingDTO s : settings) {
            SysRoleDataScope ds = new SysRoleDataScope();
            ds.setRoleId(roleId);
            ds.setResource(s.resource());
            ds.setScope(s.scope());
            roleDataScopeMapper.insert(ds);
        }
    }

    private SysRole requireExists(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private RoleVO toVO(SysRole r) {
        return new RoleVO(r.getId(), r.getName(), r.getCode());
    }
}
