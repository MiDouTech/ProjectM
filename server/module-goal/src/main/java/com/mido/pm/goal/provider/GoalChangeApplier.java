package com.mido.pm.goal.provider;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mido.pm.change.domain.ChangeApplier;
import com.mido.pm.change.entity.PmChangeRequest;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.goal.domain.GoalProgress;
import com.mido.pm.goal.entity.PmGoal;
import com.mido.pm.goal.event.GoalEvents;
import com.mido.pm.goal.mapper.PmGoalMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 目标变更应用器：变更审批通过后，把 after_payload 覆盖回 pm_goal（仅基线字段：标题/负责人/周期/量纲/起止值），
 * 进度按新基线重算（{@link GoalProgress}，与 P2 自动汇总同源），发 goal.updated。
 * 直接经 goalMapper 写表（不走 GoalService，避开 GoalService→ChangeService 的冻结校验形成环）。
 */
@Component
public class GoalChangeApplier implements ChangeApplier {

    private final PmGoalMapper goalMapper;
    private final DomainEventPublisher eventPublisher;

    public GoalChangeApplier(PmGoalMapper goalMapper, DomainEventPublisher eventPublisher) {
        this.goalMapper = goalMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public boolean supports(String bizType) {
        return "goal".equals(bizType);
    }

    @Override
    public void apply(PmChangeRequest request) {
        PmGoal g = goalMapper.selectById(request.getBizId());
        if (g == null) {
            return;
        }
        JSONObject after = JSONUtil.parseObj(request.getAfterPayload() == null ? "{}" : request.getAfterPayload());
        if (after.containsKey("title")) {
            g.setTitle(after.getStr("title"));
        }
        if (after.containsKey("ownerId")) {
            g.setOwnerId(after.getLong("ownerId"));
        }
        if (after.containsKey("period")) {
            g.setPeriod(after.getStr("period"));
        }
        if (after.containsKey("metricUnit")) {
            g.setMetricUnit(after.getStr("metricUnit"));
        }
        if (after.containsKey("metricStart")) {
            g.setMetricStart(after.getBigDecimal("metricStart"));
        }
        if (after.containsKey("metricTarget")) {
            g.setMetricTarget(after.getBigDecimal("metricTarget"));
        }
        // 基线变了，进度按新起止重算（current 不属变更范畴，沿用现值）
        g.setProgress(GoalProgress.compute(g.getMetricStart(), g.getMetricTarget(), g.getMetricCurrent()));
        goalMapper.updateById(g);
        eventPublisher.publish(GoalEvents.UPDATED, payload(g));
    }

    private Map<String, Object> payload(PmGoal g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("goalId", g.getId());
        m.put("source", "change_applied");
        return m;
    }
}
