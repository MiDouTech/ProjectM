package com.mido.pm.field.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.field.dto.FieldDefCreateDTO;
import com.mido.pm.field.dto.FieldDefUpdateDTO;
import com.mido.pm.field.dto.FieldOption;
import com.mido.pm.field.entity.PmFieldDef;
import com.mido.pm.field.mapper.PmFieldDefMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 字段定义服务单测：作用域/类型/选项/唯一键校验。 */
@ExtendWith(MockitoExtension.class)
class FieldDefServiceTest {

    @Mock
    private PmFieldDefMapper defMapper;
    private FieldDefService service;

    @BeforeEach
    void setUp() {
        service = new FieldDefService(defMapper, new ObjectMapper());
        lenient().when(defMapper.selectCount(any())).thenReturn(0L);
    }

    @Test
    void createRejectsInvalidScope() {
        assertThrows(BizException.class, () -> service.create(
                new FieldDefCreateDTO("invalid", "k1", "字段", "text", null, false, 0)));
        verify(defMapper, never()).insert(any(PmFieldDef.class));
    }

    @Test
    void createRejectsInvalidType() {
        assertThrows(BizException.class, () -> service.create(
                new FieldDefCreateDTO("task", "k1", "字段", "rating", null, false, 0)));
    }

    @Test
    void createRejectsOptionTypeWithoutOptions() {
        assertThrows(BizException.class, () -> service.create(
                new FieldDefCreateDTO("task", "k1", "下拉", "select", null, false, 0)));
    }

    @Test
    void createRejectsDuplicateOptionValue() {
        List<FieldOption> options = List.of(new FieldOption("a", "甲"), new FieldOption("a", "乙"));
        assertThrows(BizException.class, () -> service.create(
                new FieldDefCreateDTO("task", "k1", "下拉", "select", options, false, 0)));
    }

    @Test
    void createRejectsDuplicateKey() {
        when(defMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BizException.class, () -> service.create(
                new FieldDefCreateDTO("task", "dup", "字段", "text", null, false, 0)));
        verify(defMapper, never()).insert(any(PmFieldDef.class));
    }

    @Test
    void createTextOk() {
        Long id = service.create(new FieldDefCreateDTO("project", "owner", "负责人", "text", null, true, 3));
        // id 由 MP 雪花在 insert 时回填，这里 mock 不回填，仅校验流程未抛错且执行 insert
        verify(defMapper).insert(any(PmFieldDef.class));
    }

    @Test
    void updateRejectsInvalidType() {
        PmFieldDef def = new PmFieldDef();
        def.setId(1L);
        def.setScope("task");
        when(defMapper.selectById(1L)).thenReturn(def);
        assertThrows(BizException.class, () -> service.update(1L,
                new FieldDefUpdateDTO("字段", "bogus", null, null, null, null)));
    }

    @Test
    void updateMissingThrows() {
        when(defMapper.selectById(9L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.update(9L,
                new FieldDefUpdateDTO("字段", "text", null, null, null, null)));
    }
}
