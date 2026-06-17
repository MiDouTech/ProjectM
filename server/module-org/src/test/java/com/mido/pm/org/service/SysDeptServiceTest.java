package com.mido.pm.org.service;

import com.mido.pm.org.dto.DeptVO;
import com.mido.pm.org.entity.SysDept;
import com.mido.pm.org.mapper.SysDeptMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * 部门树装配逻辑单测（无 DB，mock mapper）。
 */
@ExtendWith(MockitoExtension.class)
class SysDeptServiceTest {

    @Mock
    private SysDeptMapper deptMapper;

    @InjectMocks
    private SysDeptService deptService;

    private SysDept dept(Long id, String name, Long parentId) {
        SysDept d = new SysDept();
        d.setId(id);
        d.setName(name);
        d.setParentId(parentId);
        return d;
    }

    @Test
    void treeNestsThreeLevels() {
        // 总部(根) → 研发 → 测试
        when(deptMapper.selectList(null)).thenReturn(List.of(
                dept(1L, "总部", 0L),
                dept(2L, "研发", 1L),
                dept(3L, "测试", 2L)));

        List<DeptVO> tree = deptService.tree();

        assertEquals(1, tree.size(), "应只有一个根");
        DeptVO root = tree.get(0);
        assertEquals(1L, root.getId());
        assertEquals(1, root.getChildren().size());

        DeptVO rd = root.getChildren().get(0);
        assertEquals(2L, rd.getId());
        assertEquals(1, rd.getChildren().size());
        assertEquals(3L, rd.getChildren().get(0).getId());
    }

    @Test
    void multipleRoots() {
        when(deptMapper.selectList(null)).thenReturn(List.of(
                dept(1L, "甲", 0L),
                dept(2L, "乙", null)));

        List<DeptVO> tree = deptService.tree();

        assertEquals(2, tree.size(), "parentId 为 0 或 null 均为根");
    }
}
