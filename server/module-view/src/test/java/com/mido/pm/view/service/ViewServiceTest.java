package com.mido.pm.view.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.view.dto.ViewConfig;
import com.mido.pm.view.dto.ViewSaveDTO;
import com.mido.pm.view.entity.PmView;
import com.mido.pm.view.mapper.PmViewMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 视图服务单测：scope/type/projectId/expandLevel/filters 校验与所有者权限。 */
@ExtendWith(MockitoExtension.class)
class ViewServiceTest {

    @Mock
    private PmViewMapper viewMapper;
    private ViewService service;

    private ViewConfig okConfig() {
        return new ViewConfig(null, List.of(), 1, null, List.of());
    }

    @BeforeEach
    void setUp() {
        service = new ViewService(viewMapper, new ObjectMapper());
        CurrentUser u = new CurrentUser();
        u.setUserId(7L);
        UserContext.set(u);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createRejectsInvalidScope() {
        assertThrows(BizException.class, () -> service.create(
                new ViewSaveDTO("视图", "invalid", "kanban", null, okConfig())));
        verify(viewMapper, never()).insert(any(PmView.class));
    }

    @Test
    void createRejectsInvalidType() {
        assertThrows(BizException.class, () -> service.create(
                new ViewSaveDTO("视图", "personal", "chart", null, okConfig())));
    }

    @Test
    void createRejectsProjectScopeWithoutProjectId() {
        assertThrows(BizException.class, () -> service.create(
                new ViewSaveDTO("视图", "project", "list", null, okConfig())));
    }

    @Test
    void createRejectsExpandLevelOutOfRange() {
        ViewConfig bad = new ViewConfig(null, List.of(), 6, null, List.of());
        assertThrows(BizException.class, () -> service.create(
                new ViewSaveDTO("视图", "personal", "list", null, bad)));
    }

    @Test
    void createRejectsBadFilterLogic() {
        ViewConfig bad = new ViewConfig(null, List.of(), 1,
                new ViewConfig.FilterGroup("xor", List.of()), List.of());
        assertThrows(BizException.class, () -> service.create(
                new ViewSaveDTO("视图", "personal", "list", null, bad)));
    }

    @Test
    void updateRejectsNonOwner() {
        PmView other = new PmView();
        other.setId(5L);
        other.setOwnerId(999L); // 非当前用户(7)
        when(viewMapper.selectById(5L)).thenReturn(other);
        assertThrows(BizException.class, () -> service.update(5L,
                new ViewSaveDTO("视图", "personal", "list", null, okConfig())));
        verify(viewMapper, never()).updateById(any(PmView.class));
    }

    @Test
    void getRejectsMissing() {
        when(viewMapper.selectById(404L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.get(404L));
    }
}
