package com.mido.pm.field.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.field.dto.FieldValueVO;
import com.mido.pm.field.dto.FieldValuesWriteDTO;
import com.mido.pm.field.entity.PmFieldDef;
import com.mido.pm.field.entity.PmFieldValue;
import com.mido.pm.field.mapper.PmFieldDefMapper;
import com.mido.pm.field.mapper.PmFieldValueMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 字段值服务单测：实体类型/字段有效性/作用域/必填/类型与选项校验、upsert 与活动流。 */
@ExtendWith(MockitoExtension.class)
class FieldValueServiceTest {

    @Mock
    private PmFieldValueMapper valueMapper;
    @Mock
    private PmFieldDefMapper defMapper;
    @Mock
    private AuditLogService auditLogService;
    private FieldValueService service;

    @BeforeEach
    void setUp() {
        service = new FieldValueService(valueMapper, defMapper, auditLogService, new ObjectMapper());
    }

    private PmFieldDef def(Long id, String scope, String type, boolean required, String optionsJson) {
        PmFieldDef d = new PmFieldDef();
        d.setId(id);
        d.setScope(scope);
        d.setType(type);
        d.setName("字段" + id);
        d.setFieldKey("k" + id);
        d.setRequired(required ? 1 : 0);
        d.setEnabled(1);
        d.setOptions(optionsJson);
        return d;
    }

    private FieldValuesWriteDTO write(String entityType, Long entityId, Long fieldId, String value) {
        return new FieldValuesWriteDTO(entityType, entityId,
                List.of(new FieldValuesWriteDTO.Item(fieldId, value)));
    }

    @Test
    void saveRejectsInvalidEntityType() {
        assertThrows(BizException.class, () -> service.saveValues(write("bogus", 1L, 1L, "x")));
    }

    @Test
    void saveRejectsUnknownField() {
        when(defMapper.selectById(1L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.saveValues(write("task", 1L, 1L, "x")));
    }

    @Test
    void saveRejectsDisabledField() {
        PmFieldDef d = def(1L, "task", "text", false, null);
        d.setEnabled(0);
        when(defMapper.selectById(1L)).thenReturn(d);
        assertThrows(BizException.class, () -> service.saveValues(write("task", 1L, 1L, "x")));
    }

    @Test
    void saveRejectsScopeMismatch() {
        when(defMapper.selectById(1L)).thenReturn(def(1L, "project", "text", false, null));
        assertThrows(BizException.class, () -> service.saveValues(write("task", 1L, 1L, "x")));
    }

    @Test
    void saveRejectsRequiredBlank() {
        when(defMapper.selectById(1L)).thenReturn(def(1L, "task", "text", true, null));
        assertThrows(BizException.class, () -> service.saveValues(write("task", 1L, 1L, "")));
        verify(valueMapper, never()).insert(any(PmFieldValue.class));
    }

    @Test
    void saveRejectsBadNumber() {
        when(defMapper.selectById(1L)).thenReturn(def(1L, "task", "number", false, null));
        assertThrows(BizException.class, () -> service.saveValues(write("task", 1L, 1L, "abc")));
    }

    @Test
    void saveRejectsSelectNotInOptions() {
        String opts = "[{\"value\":\"a\",\"label\":\"甲\"},{\"value\":\"b\",\"label\":\"乙\"}]";
        when(defMapper.selectById(1L)).thenReturn(def(1L, "task", "select", false, opts));
        assertThrows(BizException.class, () -> service.saveValues(write("task", 1L, 1L, "c")));
    }

    @Test
    void saveRejectsMultiSelectNotInOptions() {
        String opts = "[{\"value\":\"a\",\"label\":\"甲\"},{\"value\":\"b\",\"label\":\"乙\"}]";
        when(defMapper.selectById(1L)).thenReturn(def(1L, "task", "multi_select", false, opts));
        assertThrows(BizException.class, () -> service.saveValues(write("task", 1L, 1L, "[\"a\",\"c\"]")));
    }

    @Test
    void saveInsertsAndRecordsActivity() {
        when(defMapper.selectById(1L)).thenReturn(def(1L, "task", "number", false, null));
        when(valueMapper.selectOne(any())).thenReturn(null);
        service.saveValues(write("task", 5L, 1L, "12.50"));
        verify(valueMapper).insert(any(PmFieldValue.class));
        verify(auditLogService).record(eq("task"), eq(5L), eq("updated"), any());
    }

    @Test
    void saveSkipsUnchanged() {
        when(defMapper.selectById(1L)).thenReturn(def(1L, "task", "text", false, null));
        PmFieldValue existing = new PmFieldValue();
        existing.setValue("same");
        when(valueMapper.selectOne(any())).thenReturn(existing);
        service.saveValues(write("task", 5L, 1L, "same"));
        verify(valueMapper, never()).insert(any(PmFieldValue.class));
        verify(valueMapper, never()).updateById(any(PmFieldValue.class));
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    void getValuesReturnsDefsWithValues() {
        when(defMapper.selectList(any())).thenReturn(List.of(def(1L, "task", "text", false, null)));
        PmFieldValue v = new PmFieldValue();
        v.setFieldId(1L);
        v.setValue("hello");
        when(valueMapper.selectList(any())).thenReturn(List.of(v));
        List<FieldValueVO> result = service.getValues("task", 5L);
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).value());
        assertEquals("k1", result.get(0).fieldKey());
    }
}
