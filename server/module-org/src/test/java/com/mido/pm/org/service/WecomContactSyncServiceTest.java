package com.mido.pm.org.service;

import com.mido.pm.common.exception.BizException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 通讯录同步单测：未启用拒绝；新成员建用户+写映射；已映射成员更新不重复建。 */
@ExtendWith(MockitoExtension.class)
class WecomContactSyncServiceTest {

    @Mock private WecomContactClient contactClient;
    @Mock private SysDeptMapper deptMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private SysIdentityMapMapper identityMapMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private WecomContactSyncService service;

    @Test
    void disabledRejected() {
        when(contactClient.enabled()).thenReturn(false);
        assertThrows(BizException.class, () -> service.sync());
    }

    @Test
    void newMemberCreatesUserAndMapping() {
        when(contactClient.enabled()).thenReturn(true);
        when(contactClient.listDepartments()).thenReturn(List.of(new WecomDept(2L, "研发部", 1L)));
        when(contactClient.listMembers())
                .thenReturn(List.of(new WecomMember("zhangsan", "张三", "13800000000", List.of(2L))));
        when(deptMapper.selectOne(any())).thenReturn(null); // 部门不存在 → 新建
        when(identityMapMapper.selectOne(any())).thenReturn(null); // 未映射 → 新建用户
        lenient().when(passwordEncoder.encode(any())).thenReturn("hash");

        WecomSyncResultVO r = service.sync();

        assertEquals(1, r.deptCount());
        assertEquals(1, r.userCreated());
        assertEquals(0, r.userUpdated());
        verify(userMapper).insert(any(SysUser.class));
        verify(identityMapMapper).insert(any(SysIdentityMap.class));
    }

    @Test
    void mappedMemberUpdatesUser() {
        when(contactClient.enabled()).thenReturn(true);
        when(contactClient.listDepartments()).thenReturn(List.of());
        when(contactClient.listMembers())
                .thenReturn(List.of(new WecomMember("lisi", "李四", "13900000000", List.of())));
        SysIdentityMap mapping = new SysIdentityMap();
        mapping.setUserId(50L);
        when(identityMapMapper.selectOne(any())).thenReturn(mapping);
        when(userMapper.selectById(50L)).thenReturn(new SysUser());

        WecomSyncResultVO r = service.sync();

        assertEquals(0, r.userCreated());
        assertEquals(1, r.userUpdated());
        verify(userMapper).updateById(any(SysUser.class));
        verify(userMapper, never()).insert(any(SysUser.class));
        verify(identityMapMapper, never()).insert(any(SysIdentityMap.class));
    }

    @Test
    void deptParentResolvedRegardlessOfListOrder() {
        when(contactClient.enabled()).thenReturn(true);
        // 子部门(id=5,parent=10) 列在父部门(id=10,parent=1) 之前——父 id 大于子 id
        when(contactClient.listDepartments()).thenReturn(List.of(
                new WecomDept(5L, "测试组", 10L), new WecomDept(10L, "研发部", 1L)));
        when(contactClient.listMembers()).thenReturn(List.of());
        when(deptMapper.selectOne(any())).thenReturn(null);
        AtomicLong seq = new AtomicLong(100);
        doAnswer(inv -> {
            ((SysDept) inv.getArgument(0)).setId(seq.incrementAndGet());
            return 1;
        }).when(deptMapper).insert(any(SysDept.class));
        org.mockito.ArgumentCaptor<SysDept> cap = org.mockito.ArgumentCaptor.forClass(SysDept.class);

        service.sync();

        verify(deptMapper, times(2)).insert(cap.capture());
        Map<String, SysDept> byName = cap.getAllValues().stream()
                .collect(Collectors.toMap(SysDept::getName, x -> x));
        assertEquals(0L, byName.get("研发部").getParentId()); // 研发部挂顶级
        assertEquals(byName.get("研发部").getId(), byName.get("测试组").getParentId()); // 测试组挂研发部
    }
}
