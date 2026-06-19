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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 目标变更应用器：变更审批通过后，把 after_payload 覆盖回 pm_goal（仅基线字段：标题/负责人/周期/量纲/起止值），
 * 进度按新基线重算（{@link GoalProgress}，与 P2 自动汇总同源），发 goal.updated。
 * 直接经 goalMapper 写表（不走 GoalService，避开 GoalService→ChangeService 的冻结校验形成环）。
 */
@Component
public class GoalChangeApplier implements ChangeApplier {

    private static final Logger log = LoggerFactory.getLogger(GoalChangeApplier.class);

    /** JSON 字段名 → 写入 PmGoal 的处理器。新增基线字段只动这一处；未知字段记 warn(暴露与 GoalChangeService 的漂移)。 */
    private static final Map<String, BiConsumer<PmGoal, JSONObject>> APPLIERS = Map.of(
            "title", (g, a) -> g.setTitle(a.getStr("title")),
            "ownerId", (g, a) -> g.setOwnerId(a.getLong("ownerId")),
            "period", (g, a) -> g.setPeriod(a.getStr("period")),
            "metricUnit", (g, a) -> g.setMetricUnit(a.getStr("metricUnit")),
            "metricStart", (g, a) -> g.setMetricStart(a.getBigDecimal("metricStart")),
            "metricTarget", (g, a) -> g.setMetricTarget(a.getBigDecimal("metricTarget")));

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
        for (String key : after.keySet()) {
            BiConsumer<PmGoal, JSONObject> handler = APPLIERS.get(key);
            if (handler == null) {
                log.warn("目标变更含未知字段，已忽略：changeId={}, field={}", request.getId(), key);
                continue;
            }
            handler.accept(g, after);
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
