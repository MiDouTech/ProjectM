package com.mido.pm.goal.provider;

import com.mido.pm.change.entity.PmChangeRequest;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.goal.entity.PmGoal;
import com.mido.pm.goal.event.GoalEvents;
import com.mido.pm.goal.mapper.PmGoalMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 目标变更应用：after_payload 覆盖基线字段，进度按新起止重算，发 goal.updated。 */
@ExtendWith(MockitoExtension.class)
class GoalChangeApplierTest {

    @Mock private PmGoalMapper goalMapper;
    @Mock private DomainEventPublisher publisher;

    @Test
    void overlaysFieldsAndRecomputesProgress() {
        PmGoal g = new PmGoal();
        g.setId(7L);
        g.setTitle("旧标题");
        g.setMetricStart(BigDecimal.ZERO);
        g.setMetricTarget(BigDecimal.valueOf(100));
        g.setMetricCurrent(BigDecimal.valueOf(50));
        when(goalMapper.selectById(7L)).thenReturn(g);

        PmChangeRequest cr = new PmChangeRequest();
        cr.setBizType("goal");
        cr.setBizId(7L);
        cr.setAfterPayload("{\"title\":\"新标题\",\"metricTarget\":200}");

        new GoalChangeApplier(goalMapper, publisher).apply(cr);

        assertEquals("新标题", g.getTitle());
        assertEquals(0, g.getMetricTarget().compareTo(BigDecimal.valueOf(200)));
        // 进度按新基线重算：current 50 / (target 200 - start 0) = 25%
        assertEquals(0, g.getProgress().compareTo(BigDecimal.valueOf(25)));
        verify(goalMapper).updateById(g);
        verify(publisher).publish(eq(GoalEvents.UPDATED), any());
    }
}
