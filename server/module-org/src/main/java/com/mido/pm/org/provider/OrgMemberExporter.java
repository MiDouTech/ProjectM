package com.mido.pm.org.provider;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.tenant.TenantDataExporter;
import com.mido.pm.org.entity.SysUser;
import com.mido.pm.org.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 导出当前租户成员（脱敏：不含密码哈希）。 */
@Component
public class OrgMemberExporter implements TenantDataExporter {

    private final SysUserMapper userMapper;

    public OrgMemberExporter(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public String domain() {
        return "members";
    }

    @Override
    public Object exportData() {
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery());
        return users.stream().map(u -> Map.of(
                "id", String.valueOf(u.getId()),
                "username", nullToEmpty(u.getUsername()),
                "name", nullToEmpty(u.getName()),
                "phone", nullToEmpty(u.getPhone()),
                "deptId", u.getDeptId() == null ? "" : String.valueOf(u.getDeptId()),
                "jobLevel", nullToEmpty(u.getJobLevel()),
                "status", nullToEmpty(u.getStatus()))).toList();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
