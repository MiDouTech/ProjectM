package com.mido.pm.briefing.service;

import com.mido.pm.briefing.dto.IssueCreateDTO;
import com.mido.pm.briefing.entity.PmBriefing;
import com.mido.pm.briefing.entity.PmBriefingIssue;
import com.mido.pm.briefing.event.BriefingEvents;
import com.mido.pm.briefing.mapper.PmBriefingIssueMapper;
import com.mido.pm.briefing.mapper.PmBriefingMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
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

/** 跟进问题单测：可见者方可提问题、关闭发事件、越权拒绝。 */
@ExtendWith(MockitoExtension.class)
class BriefingIssueServiceTest {

    private static final long ME = 100L;

    @Mock private PmBriefingIssueMapper issueMapper;
    @Mock private PmBriefingMapper briefingMapper;
    @Mock private BriefingReviewService reviewService;
    @Mock private DomainEventPublisher eventPublisher;
    @InjectMocks private BriefingIssueService service;

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

    private PmBriefing briefing(Long author) {
        PmBriefing b = new PmBriefing();
        b.setId(9L);
        b.setAuthorId(author);
        return b;
    }

    @Test
    void authorCanRaiseIssue() {
        when(briefingMapper.selectById(9L)).thenReturn(briefing(ME));
        service.create(new IssueCreateDTO(9L, "跟进采购", null, null));
        verify(issueMapper).insert(any(PmBriefingIssue.class));
        verify(eventPublisher).publish(eq(BriefingEvents.ISSUE_RAISED), any());
    }

    @Test
    void nonVisibleUserRejected() {
        when(briefingMapper.selectById(9L)).thenReturn(briefing(200L));
        when(reviewService.isReviewer(9L, ME)).thenReturn(false);
        assertThrows(BizException.class,
                () -> service.create(new IssueCreateDTO(9L, "x", null, null)));
        verify(issueMapper, never()).insert(any(PmBriefingIssue.class));
    }

    @Test
    void closingPublishesClosedEvent() {
        PmBriefingIssue issue = new PmBriefingIssue();
        issue.setId(5L);
        issue.setRaisedBy(ME);
        when(issueMapper.selectById(5L)).thenReturn(issue);
        service.updateStatus(5L, "closed");
        verify(eventPublisher).publish(eq(BriefingEvents.ISSUE_CLOSED), any());
    }

    @Test
    void updateStatusRejectsOutsider() {
        PmBriefingIssue issue = new PmBriefingIssue();
        issue.setRaisedBy(200L);
        issue.setOwnerId(300L);
        when(issueMapper.selectById(5L)).thenReturn(issue);
        assertThrows(BizException.class, () -> service.updateStatus(5L, "following"));
    }
}
