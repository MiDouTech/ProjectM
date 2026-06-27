package com.mido.pm.project.service;

import com.mido.pm.project.dto.PortfolioOverviewVO;
import com.mido.pm.project.dto.PortfolioSaveDTO;
import com.mido.pm.project.dto.ProjectVO;
import com.mido.pm.project.entity.PmPortfolio;
import com.mido.pm.project.entity.PmPortfolioProject;
import com.mido.pm.project.mapper.PmPortfolioMapper;
import com.mido.pm.project.mapper.PmPortfolioProjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 项目集服务单测：创建落库；总览按可见项目做状态汇总。
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock private PmPortfolioMapper portfolioMapper;
    @Mock private PmPortfolioProjectMapper linkMapper;
    @Mock private com.mido.pm.project.mapper.PmPortfolioMemberMapper memberMapper;
    @Mock private ProjectService projectService;

    private PortfolioService service() {
        return new PortfolioService(portfolioMapper, linkMapper, memberMapper, projectService);
    }

    private ProjectVO project(Long id, String status) {
        return new ProjectVO(id, "C" + id, "项目" + id, null, null, null, null, null, status,
                null, null, null, null, null, null, null, null, null, 0);
    }

    @Test
    void createPersistsPortfolioAndAddsOwnerAsMember() {
        when(memberMapper.selectList(any())).thenReturn(List.of());
        service().create(new PortfolioSaveDTO("年度战略", "desc", 9L, null, null));
        verify(portfolioMapper).insert(any(PmPortfolio.class));
        // 创建人(9)默认成为成员
        verify(memberMapper).insert(any(com.mido.pm.project.entity.PmPortfolioMember.class));
    }

    @Test
    void overviewAggregatesVisibleProjectsByStatus() {
        PmPortfolio p = new PmPortfolio();
        p.setId(1L);
        p.setName("年度战略");
        when(portfolioMapper.selectById(1L)).thenReturn(p);
        PmPortfolioProject l1 = new PmPortfolioProject();
        l1.setProjectId(10L);
        PmPortfolioProject l2 = new PmPortfolioProject();
        l2.setProjectId(11L);
        when(linkMapper.selectList(any())).thenReturn(List.of(l1, l2));
        when(projectService.visibleByIds(anyList()))
                .thenReturn(List.of(project(10L, "进行中"), project(11L, "进行中")));

        PortfolioOverviewVO ov = service().overview(1L);

        assertEquals(2, ov.projects().size());
        assertEquals(2L, ov.statusCount().get("进行中"));
    }
}
