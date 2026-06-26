package com.mido.pm.verify.service;

import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.project.service.ProjectService;
import com.mido.pm.stakeholder.dto.StakeholderVO;
import com.mido.pm.stakeholder.service.StakeholderService;
import com.mido.pm.verify.dto.ScoreSubmitDTO;
import com.mido.pm.verify.entity.PmNpssReview;
import com.mido.pm.verify.entity.PmNpssScore;
import com.mido.pm.verify.mapper.PmNpssReviewMapper;
import com.mido.pm.verify.mapper.PmNpssScoreMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Mock private NpssSubjectService npssSubjectService;
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
    void startReviewReentrantWhenPendingExists() {
        // 已有进行中轮次 → 重复扫描不重复建 review、不再流转项目
        PmNpssReview existing = pendingReview();
        when(reviewMapper.selectList(any())).thenReturn(java.util.List.of(existing));

        Long id = service.startReview(100L);

        org.junit.jupiter.api.Assertions.assertEquals(1L, id);
        verify(projectService, never()).transition(any(), any());
        verify(reviewMapper, never()).insert(any(PmNpssReview.class));
    }

    @Test
    void startReviewEmitsEventWithStakeholderRecipients() {
        when(reviewMapper.selectList(any())).thenReturn(java.util.List.of()); // 无进行中轮次
        when(stakeholderService.list(100L)).thenReturn(java.util.List.of(
                new StakeholderVO(1L, 100L, 8L, null, "owner", "internal", 3, 3, new BigDecimal("0.5")),
                new StakeholderVO(2L, 100L, 9L, null, "user", "internal", 2, 2, new BigDecimal("0.5")),
                new StakeholderVO(3L, 100L, null, "外部客户", "client", "external", 1, 3, new BigDecimal("0.0"))));

        service.startReview(100L);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publish(eq("npss.review.started"), captor.capture());
        // 仅带 userId 的干系人进收件人列表（外部无账号者不通知）
        assertEquals(List.of(8L, 9L), captor.getValue().get("recipientUserIds"));
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

    @Test
    void startReviewSubjectModeBuildsTodosPerMember() {
        when(reviewMapper.selectList(any())).thenReturn(java.util.List.of()); // 无进行中轮次
        when(stakeholderService.list(100L)).thenReturn(java.util.List.of(
                new StakeholderVO(1L, 100L, 8L, null, "sponsor", "internal", 3, 3, null),
                new StakeholderVO(2L, 100L, 9L, null, "business", "internal", 2, 2, null)));
        // 单主体含两成员 → 建 2 条评分待办，收件人去重为 [8,9]
        when(npssSubjectService.subjectsForReview(100L)).thenReturn(java.util.List.of(
                new NpssSubjectService.MaterializedSubject(10L, new BigDecimal("100"), true,
                        java.util.List.of(1L, 2L))));

        service.startReview(100L);

        verify(scoreMapper, org.mockito.Mockito.times(2)).insert(any(PmNpssScore.class));
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publish(eq("npss.review.started"), captor.capture());
        assertEquals(List.of(8L, 9L), captor.getValue().get("recipientUserIds"));
        assertEquals(2, captor.getValue().get("stakeholderCount"));
    }

    @Test
    void summarizeSubjectModeAveragesThenWeights() {
        // 主体A 权重30 成员[9,10]→平均9.5；主体B 权重70 成员[8] → 84.50 → mixed
        when(scoreMapper.selectList(any())).thenReturn(java.util.List.of(
                subjectScore(10L, "30", 9), subjectScore(10L, "30", 10), subjectScore(20L, "70", 8)));

        PmNpssReview review = pendingReview();
        service.summarize(review);

        assertEquals(0, new BigDecimal("84.50").compareTo(review.getWeightedScore()));
        org.junit.jupiter.api.Assertions.assertEquals("mixed", review.getResultLevel());
        verify(eventPublisher).publish(eq("npss.review.completed"), any());
    }

    private PmNpssScore subjectScore(Long subjectId, String weight, int score) {
        PmNpssScore s = new PmNpssScore();
        s.setReviewId(1L);
        s.setSubjectId(subjectId);
        s.setWeight(new BigDecimal(weight));
        s.setScore(score);
        return s;
    }
}
