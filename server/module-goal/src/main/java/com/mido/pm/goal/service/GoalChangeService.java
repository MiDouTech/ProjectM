package com.mido.pm.goal.service;

import cn.hutool.json.JSONUtil;
import com.mido.pm.change.dto.ChangeRequestVO;
import com.mido.pm.change.dto.ChangeSubmitCmd;
import com.mido.pm.change.service.ChangeService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.goal.dto.GoalChangeRequestDTO;
import com.mido.pm.goal.entity.PmGoal;
import com.mido.pm.goal.mapper.PmGoalMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 目标变更发起：构建变更前后快照，提交变更中心（{@link ChangeService}，复用审批引擎）。
 * 目标域负责目标字段的快照与拟改值组装，变更域不耦合目标字段。
 */
@Service
public class GoalChangeService {

    /** 被改实体域标识（变更台账 biz_type / ChangeApplier.supports）。 */
    public static final String BIZ_TYPE = "goal";

    // 注：goal_close(目标关闭)暂不支持——pm_goal 无状态列(属表结构决策)，且其不改任何基线字段，
    //     经变更流程提交会被「无变更内容」拦截，故先不开放，待目标状态机落地再纳入。
    private static final Set<String> CHANGE_TYPES =
            Set.of("goal_target", "goal_scope", "goal_owner", "goal_period");

    /** 可变更基线字段：现值取值器(快照) + 拟改值取值器(DTO)。新增字段只动这一处（before/after 同源，杜绝漂移）。 */
    private record Field(String key, Function<PmGoal, Object> current, Function<GoalChangeRequestDTO, Object> proposed) {
    }

    private static final List<Field> FIELDS = List.of(
            new Field("title", PmGoal::getTitle, GoalChangeRequestDTO::title),
            new Field("ownerId", PmGoal::getOwnerId, GoalChangeRequestDTO::ownerId),
            new Field("period", PmGoal::getPeriod, GoalChangeRequestDTO::period),
            new Field("metricUnit", PmGoal::getMetricUnit, GoalChangeRequestDTO::metricUnit),
            new Field("metricStart", PmGoal::getMetricStart, GoalChangeRequestDTO::metricStart),
            new Field("metricTarget", PmGoal::getMetricTarget, GoalChangeRequestDTO::metricTarget));

    private final PmGoalMapper goalMapper;
    private final ChangeService changeService;

    public GoalChangeService(PmGoalMapper goalMapper, ChangeService changeService) {
        this.goalMapper = goalMapper;
        this.changeService = changeService;
    }

    /** 发起目标变更：组装 before/after 快照后提交变更中心，返回变更单 id。 */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long goalId, GoalChangeRequestDTO req) {
        if (!CHANGE_TYPES.contains(req.changeType())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法变更类型: " + req.changeType());
        }
        PmGoal g = goalMapper.selectById(goalId);
        if (g == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "目标不存在");
        }
        // before(基线快照) 与 after(改动项) 由同一字段表派生，避免两份手抄清单漂移
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();
        for (Field f : FIELDS) {
            Object cur = f.current().apply(g);
            before.put(f.key(), cur);
            Object proposed = f.proposed().apply(req);
            if (proposed != null && !sameValue(proposed, cur)) {
                after.put(f.key(), proposed);
            }
        }
        if (after.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "拟改值与现值一致，无变更内容");
        }
        // 审批路由/展示上下文
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("changeType", req.changeType());
        formData.put("goalId", goalId);
        formData.put("goalTitle", g.getTitle());

        ChangeSubmitCmd cmd = new ChangeSubmitCmd(BIZ_TYPE, goalId, req.changeType(),
                "目标变更·" + g.getTitle(), req.reason(), req.impact(),
                JSONUtil.toJsonStr(before), JSONUtil.toJsonStr(after), formData);
        return changeService.submit(cmd);
    }

    /** 某目标的变更历史（变更中心·目标视角）。 */
    public List<ChangeRequestVO> list(Long goalId) {
        return changeService.list(BIZ_TYPE, goalId, null);
    }

    /** 等值判断：BigDecimal 按数值(compareTo)比较，避免 100 与 100.00 标度差异误判为变更。 */
    private boolean sameValue(Object proposed, Object current) {
        if (current == null) {
            return false;
        }
        if (proposed instanceof BigDecimal p && current instanceof BigDecimal c) {
            return p.compareTo(c) == 0;
        }
        return proposed.toString().equals(current.toString());
    }
}
