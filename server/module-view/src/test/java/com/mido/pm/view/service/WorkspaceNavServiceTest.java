package com.mido.pm.view.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.view.dto.NavItemSaveDTO;
import com.mido.pm.view.dto.NavNodeVO;
import com.mido.pm.view.entity.PmModuleNav;
import com.mido.pm.view.mapper.PmModuleNavMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作区导航解析单测：空配置回落内置默认；有配置按编排；非法模块/组件拒绝。
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceNavServiceTest {

    @Mock
    private PmModuleNavMapper navMapper;

    private WorkspaceNavService service() {
        return new WorkspaceNavService(navMapper);
    }

    @Test
    void resolve_emptyConfig_fallsBackToDefault() {
        when(navMapper.selectList(any())).thenReturn(List.of());
        List<NavNodeVO> nav = service().resolve("project");
        // 内置默认含 全部项目 + 项目集
        assertEquals(2, nav.size());
        assertTrue(nav.stream().anyMatch(n -> "portfolios".equals(n.code())));
    }

    @Test
    void resolve_withConfig_usesOrchestrationAndRename() {
        PmModuleNav row = new PmModuleNav();
        row.setModule("project");
        row.setComponentCode("projects");
        row.setDisplayName("我的项目库");
        row.setSort(0);
        row.setEnabled(1);
        when(navMapper.selectList(any())).thenReturn(List.of(row));

        List<NavNodeVO> nav = service().resolve("project");
        assertEquals(1, nav.size());
        assertEquals("我的项目库", nav.get(0).name(), "改名应生效");
        assertEquals("/project", nav.get(0).route(), "route 回落 catalog");
    }

    @Test
    void resolve_unknownModule_rejected() {
        assertThrows(BizException.class, () -> service().resolve("nope"));
    }

    @Test
    void saveNav_unknownComponent_rejected() {
        assertThrows(BizException.class, () -> service().saveNav("project",
                List.of(new NavItemSaveDTO("ghost", null, null, null, true))));
    }

    @Test
    void saveNav_replacesAll() {
        service().saveNav("project", List.of(new NavItemSaveDTO("projects", null, null, null, true)));
        verify(navMapper).delete(any());
        verify(navMapper).insert(any(PmModuleNav.class));
    }

    @Test
    void resolve_disabledItem_hidden() {
        PmModuleNav a = new PmModuleNav();
        a.setModule("project"); a.setComponentCode("projects"); a.setSort(0); a.setEnabled(1);
        PmModuleNav b = new PmModuleNav();
        b.setModule("project"); b.setComponentCode("portfolios"); b.setSort(1); b.setEnabled(0);
        when(navMapper.selectList(any())).thenReturn(List.of(a, b));
        List<NavNodeVO> nav = service().resolve("project");
        assertEquals(1, nav.size());
        assertFalse(nav.stream().anyMatch(n -> "portfolios".equals(n.code())), "停用项应隐藏");
    }
}
