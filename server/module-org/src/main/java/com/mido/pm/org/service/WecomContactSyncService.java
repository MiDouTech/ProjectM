package com.mido.pm.org.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.org.dto.WecomSyncResultVO;
import com.mido.pm.org.entity.SysDept;
import com.mido.pm.org.entity.SysIdentityMap;
import com.mido.pm.org.entity.SysUser;
import com.mido.pm.org.mapper.SysDeptMapper;
import com.mido.pm.org.mapper.SysIdentityMapMapper;
import com.mido.pm.org.mapper.SysUserMapper;
import com.mido.pm.provider.identity.WecomContactClient;
import com.mido.pm.provider.identity.WecomDept;
import com.mido.pm.provider.identity.WecomMember;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 企微通讯录同步：拉企微部门/成员 upsert 到 sys_dept/sys_user，并写 sys_identity_map(provider='wecom')。
 *
 * <p>在触发管理员的租户内写入。部门按 name+parentId 幂等；成员按 sys_identity_map 幂等。
 * 企微成员登录走 SSO/本地（新建用户设随机密码，不可用作密码登录）。全量 upsert，不处理离职删除（后续）。</p>
 */
@Service
public class WecomContactSyncService {

    private static final String PROVIDER_WECOM = "wecom";
    private static final long WECOM_ROOT_DEPT = 1L;
    private static final long LOCAL_ROOT_PARENT = 0L;

    private final WecomContactClient contactClient;
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final SysIdentityMapMapper identityMapMapper;
    private final PasswordEncoder passwordEncoder;

    public WecomContactSyncService(WecomContactClient contactClient, SysDeptMapper deptMapper,
                                   SysUserMapper userMapper, SysIdentityMapMapper identityMapMapper,
                                   PasswordEncoder passwordEncoder) {
        this.contactClient = contactClient;
        this.deptMapper = deptMapper;
        this.userMapper = userMapper;
        this.identityMapMapper = identityMapMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(rollbackFor = Exception.class)
    public WecomSyncResultVO sync() {
        if (!contactClient.enabled()) {
            throw new BizException(ErrorCode.FORBIDDEN, "企微通讯录同步未启用");
        }
        List<WecomDept> depts = contactClient.listDepartments();
        Map<Long, Long> deptIdMap = upsertDepts(depts);
        int[] uc = upsertMembers(contactClient.listMembers(), deptIdMap);
        return new WecomSyncResultVO(depts.size(), uc[0], uc[1]);
    }

    /**
     * upsert 部门，返回 企微部门 id → 本地部门 id 映射（企微根部门 1 → 本地顶级 0）。
     * 按依赖关系递归解析父部门，不依赖企微返回顺序或 id 大小（父 id 未必小于子 id）。
     */
    private Map<Long, Long> upsertDepts(List<WecomDept> depts) {
        Map<Long, Long> map = new HashMap<>();
        map.put(WECOM_ROOT_DEPT, LOCAL_ROOT_PARENT);
        Map<Long, WecomDept> byId = new HashMap<>();
        for (WecomDept d : depts) {
            byId.put(d.id(), d);
        }
        for (WecomDept d : depts) {
            resolveDept(d.id(), byId, map);
        }
        return map;
    }

    /** 递归解析并 upsert 单个企微部门，返回其本地部门 id；已解析的走缓存，防环（缺父→挂顶级）。 */
    private Long resolveDept(long wecomId, Map<Long, WecomDept> byId, Map<Long, Long> map) {
        if (map.containsKey(wecomId)) {
            return map.get(wecomId);
        }
        WecomDept d = byId.get(wecomId);
        if (d == null) {
            return LOCAL_ROOT_PARENT;
        }
        map.put(wecomId, LOCAL_ROOT_PARENT); // 占位防环：环或缺父先挂顶级，下方覆盖为真实 id
        Long localParent;
        if (d.parentId() == WECOM_ROOT_DEPT || d.parentId() == 0) {
            localParent = LOCAL_ROOT_PARENT;
        } else {
            localParent = resolveDept(d.parentId(), byId, map);
        }
        SysDept existing = deptMapper.selectOne(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getName, d.name())
                .eq(SysDept::getParentId, localParent)
                .last("limit 1"));
        Long localId;
        if (existing != null) {
            localId = existing.getId();
        } else {
            SysDept dept = new SysDept();
            dept.setName(d.name());
            dept.setParentId(localParent);
            deptMapper.insert(dept);
            localId = dept.getId();
        }
        map.put(wecomId, localId);
        return localId;
    }

    /** upsert 成员；返回 [created, updated]。 */
    private int[] upsertMembers(List<WecomMember> members, Map<Long, Long> deptIdMap) {
        // 预加载本租户全部企微身份映射，避免每个成员一次 selectOne(N+1)
        Map<String, Long> externalToUserId = identityMapMapper.selectList(Wrappers.<SysIdentityMap>lambdaQuery()
                        .eq(SysIdentityMap::getProvider, PROVIDER_WECOM))
                .stream().collect(java.util.stream.Collectors.toMap(SysIdentityMap::getExternalId,
                        SysIdentityMap::getUserId, (a, b) -> a));
        int created = 0;
        int updated = 0;
        for (WecomMember m : members) {
            Long localDept = m.departmentIds() == null || m.departmentIds().isEmpty()
                    ? null : deptIdMap.get(m.departmentIds().get(0));
            Long mappedUserId = externalToUserId.get(m.userId());
            if (mappedUserId != null) {
                SysUser u = userMapper.selectById(mappedUserId);
                if (u != null) {
                    u.setName(m.name());
                    if (m.mobile() != null && !m.mobile().isBlank()) {
                        u.setPhone(m.mobile());
                    }
                    if (localDept != null) {
                        u.setDeptId(localDept);
                    }
                    userMapper.updateById(u);
                    updated++;
                }
            } else {
                SysUser u = new SysUser();
                u.setUsername(m.userId());
                u.setName(m.name());
                u.setPhone(m.mobile());
                u.setDeptId(localDept);
                u.setStatus("active");
                u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                userMapper.insert(u);

                SysIdentityMap im = new SysIdentityMap();
                im.setUserId(u.getId());
                im.setProvider(PROVIDER_WECOM);
                im.setExternalId(m.userId());
                identityMapMapper.insert(im);
                created++;
            }
        }
        return new int[]{created, updated};
    }
}
