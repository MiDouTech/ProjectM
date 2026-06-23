package com.mido.pm.briefing.service;

import com.mido.pm.briefing.dto.BriefingReviewDTO;
import com.mido.pm.briefing.entity.PmBriefing;
import com.mido.pm.briefing.entity.PmBriefingRecipient;
import com.mido.pm.briefing.entity.PmBriefingReview;
import com.mido.pm.briefing.event.BriefingEvents;
import com.mido.pm.briefing.mapper.PmBriefingMapper;
import com.mido.pm.briefing.mapper.PmBriefingRecipientMapper;
import com.mido.pm.briefing.mapper.PmBriefingReviewMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.org.service.SysDeptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 简报评审单测：评审人解析落地、非评审人不可批注、评审人批注发事件。 */
@ExtendWith(MockitoExtension.class)
class BriefingReviewServiceTest {

    private static final long ME = 100L;

    @Mock private PmBriefingMapper briefingMapper;
    @Mock private PmBriefingRecipientMapper recipientMapper;
    @Mock private PmBriefingReviewMapper reviewMapper;
    @Mock private SysDeptService deptService;
    @Mock private DomainEventPublisher eventPublisher;
    @InjectMocks private BriefingReviewService service;

    @BeforeEach
    void setUp() {
        CurrentUser u = new CurrentUser();
        u.setUserId(ME);
        UserContext.set(u);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private PmBriefing briefing(Long authorId, Long deptId) {
        PmBriefing b = new PmBriefing();
        b.setId(9L);
        b.setAuthorId(authorId);
        b.setDeptId(deptId);
        return b;
    }

    @Test
    void assignReviewerInsertsDeptLeader() {
        PmBriefing b = briefing(200L, 7L);
        when(deptService.leaderOf(7L)).thenReturn(300L);
        when(recipientMapper.selectCount(any())).thenReturn(0L);

        service.assignReviewer(b);

        verify(recipientMapper).insert(any(PmBriefingRecipient.class));
    }

    @Test
    void assignReviewerSkipsWhenLeaderIsAuthor() {
        PmBriefing b = briefing(300L, 7L);
        when(deptService.leaderOf(7L)).thenReturn(300L); // 负责人即作者本人

        service.assignReviewer(b);

        verify(recipientMapper, never()).insert(any(PmBriefingRecipient.class));
    }

    @Test
    void addReviewRejectsNonReviewer() {
        when(briefingMapper.selectById(9L)).thenReturn(briefing(200L, 7L));
        when(recipientMapper.selectCount(any())).thenReturn(0L); // 我不是评审人

        assertThrows(BizException.class, () -> service.addReview(9L, new BriefingReviewDTO("不错", null)));
        verify(reviewMapper, never()).insert(any(PmBriefingReview.class));
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void addReviewByReviewerInsertsAndPublishes() {
        when(briefingMapper.selectById(9L)).thenReturn(briefing(200L, 7L));
        when(recipientMapper.selectCount(any())).thenReturn(1L); // 我是评审人

        service.addReview(9L, new BriefingReviewDTO("继续保持", "approve"));

        verify(reviewMapper).insert(any(PmBriefingReview.class));
        verify(eventPublisher).publish(eq(BriefingEvents.REVIEWED), any());
    }
}
