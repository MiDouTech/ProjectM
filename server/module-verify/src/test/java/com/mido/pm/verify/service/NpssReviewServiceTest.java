package com.mido.pm.verify.service;

import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.project.service.ProjectService;
import com.mido.pm.provider.message.MessageProvider;
import com.mido.pm.stakeholder.service.StakeholderService;
import com.mido.pm.verify.dto.ScoreSubmitDTO;
import com.mido.pm.verify.entity.PmNpssReview;
import com.mido.pm.verify.entity.PmNpssScore;
import com.mido.pm.verify.mapper.PmNpssReviewMapper;
import com.mido.pm.verify.mapper.PmNpssScoreMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 评分幂等单测（npss-rule 防重复打分）：同一 (review, stakeholder) 已打分则重复提交不再写库/发事件。
 */
@ExtendWith(MockitoExtension.class)
class NpssReviewServiceTest {

    @Mock private PmNpssReviewMapper reviewMapper;
    @Mock private PmNpssScoreMapper scoreMapper;
    @Mock private ProjectService projectService;
    @Mock private StakeholderService stakeholderService;
    @Mock private MessageProvider messageProvider;
    @Mock private DomainEventPublisher eventPublisher;
    @InjectMocks private NpssReviewService service;

    private PmNpssReview pendingReview() {
        PmNpssReview r = new PmNpssReview();
        r.setId(1L);
        r.setProjectId(100L);
        r.setStatus("pending");
        return r;
    }

    private PmNpssScore scoreRow(Integer score) {
        PmNpssScore s = new PmNpssScore();
        s.setId(50L);
        s.setReviewId(1L);
        s.setStakeholderId(7L);
        s.setScore(score);
        s.setWeight(new BigDecimal("1"));
        return s;
    }

    @Test
    void duplicateSubmitIsIdempotent() {
        when(reviewMapper.selectById(1L)).thenReturn(pendingReview());
        when(scoreMapper.selectOne(any())).thenReturn(scoreRow(8)); // 已打分

        service.submitScore(1L, new ScoreSubmitDTO(7L, 9, "改判"));

        // 幂等：不重复写库、不重复发事件
        verify(scoreMapper, never()).updateById(any(PmNpssScore.class));
        verify(eventPublisher, never()).publish(eq("npss.scored"), any());
    }

    @Test
    void firstSubmitWritesAndEmits() {
        when(reviewMapper.selectById(1L)).thenReturn(pendingReview());
        when(scoreMapper.selectOne(any())).thenReturn(scoreRow(null)); // 待打分
        when(scoreMapper.selectCount(any())).thenReturn(1L);           // 仍有未打分 → 不触发汇总

        service.submitScore(1L, new ScoreSubmitDTO(7L, 9, "价值达预期"));

        verify(scoreMapper).updateById(any(PmNpssScore.class));
        verify(eventPublisher).publish(eq("npss.scored"), any());
    }
}
