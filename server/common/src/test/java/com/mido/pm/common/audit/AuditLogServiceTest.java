package com.mido.pm.common.audit;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审计/活动流服务单测（mock mapper，无 DB）：
 * record 落库字段正确、detail 经共享 ObjectMapper 序列化；query 分页映射为 ActivityVO。
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuditLogService service() {
        return new AuditLogService(auditLogMapper, objectMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void recordWritesEntityRefActionAndOperator() {
        CurrentUser user = new CurrentUser();
        user.setUserId(7L);
        UserContext.set(user);

        service().record(AuditActions.TARGET_PROJECT, 42L, AuditActions.STATUS_CHANGED,
                Map.of("from", "草稿", "to", "审批中"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("project", saved.getTarget());
        assertEquals(42L, saved.getTargetId());
        assertEquals("status_changed", saved.getAction());
        assertEquals(7L, saved.getUserId());
        // detail 为 JSON 串，含 from/to
        assertEquals(true, saved.getDetail().contains("审批中"));
    }

    @Test
    void recordWithoutLoginKeepsOperatorNull() {
        service().record(AuditActions.TARGET_TASK, 9L, AuditActions.CREATED, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(null, saved.getUserId());
        assertEquals(null, saved.getDetail());
    }

    @Test
    void recordWithModulePersistsModuleField() {
        service().record(AuditActions.MODULE_PERMISSION, AuditActions.TARGET_ROLE, 1L,
                AuditActions.PERMS_CHANGED, Map.of("from", List.of(), "to", List.of("a")));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(AuditActions.MODULE_PERMISSION, saved.getModule());
        assertEquals(AuditActions.TARGET_ROLE, saved.getTarget());
        assertEquals(AuditActions.PERMS_CHANGED, saved.getAction());
        assertEquals(1L, saved.getTargetId());
    }

    @Test
    void queryLogsMapsModuleAndIpToVo() {
        AuditLog row = new AuditLog();
        row.setId(99L);
        row.setUserId(5L);
        row.setModule(AuditActions.MODULE_MEMBER);
        row.setAction(AuditActions.MEMBER_ADDED);
        row.setTarget(AuditActions.TARGET_PROJECT_MEMBER);
        row.setTargetId(3L);
        row.setIp("1.2.3.4");
        row.setCreateTime(LocalDateTime.now());
        Page<AuditLog> page = new Page<>(1, 20);
        page.setRecords(List.of(row));
        page.setTotal(1);
        when(auditLogMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<AuditLogVO> result = service().queryLogs(
                new AuditLogQueryDTO(null, null, null, null, null, null, null, 1L, 20L));

        assertEquals(1, result.getList().size());
        AuditLogVO vo = result.getList().get(0);
        assertEquals(99L, vo.id());
        assertEquals(AuditActions.MODULE_MEMBER, vo.module());
        assertEquals("1.2.3.4", vo.ip());
    }

    @Test
    void queryMapsRecordsToActivityVoWithParsedDetail() {
        AuditLog row = new AuditLog();
        row.setId(100L);
        row.setUserId(7L);
        row.setAction(AuditActions.STATUS_CHANGED);
        row.setTarget("project");
        row.setTargetId(42L);
        row.setDetail("{\"from\":\"草稿\",\"to\":\"审批中\"}");
        row.setCreateTime(LocalDateTime.now());

        Page<AuditLog> page = new Page<>(1, 20);
        page.setRecords(List.of(row));
        page.setTotal(1);
        when(auditLogMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<ActivityVO> result = service().query("project", 42L, 1L, 20L);

        assertEquals(1, result.getTotal());
        ActivityVO vo = result.getList().get(0);
        assertEquals(100L, vo.id());
        assertEquals("status_changed", vo.action());
        assertInstanceOf(Map.class, vo.detail());
        assertEquals("审批中", ((Map<?, ?>) vo.detail()).get("to"));
    }
}
