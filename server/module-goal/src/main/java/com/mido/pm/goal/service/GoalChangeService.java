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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 目标变更发起：构建变更前后快照，提交变更中心（{@link ChangeService}，复用审批引擎）。
 * 目标域负责目标字段的快照与拟改值组装，变更域不耦合目标字段。
 */
@Service
public class GoalChangeService {

    /** 被改实体域标识（变更台账 biz_type / ChangeApplier.supports）。 */
    public static final String BIZ_TYPE = "goal";

    private static final Set<String> CHANGE_TYPES =
            Set.of("goal_target", "goal_scope", "goal_owner", "goal_period", "goal_close");

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
        Map<String, Object> after = new LinkedHashMap<>();
        putIfChanged(after, "title", req.title(), g.getTitle());
        putIfChanged(after, "ownerId", req.ownerId(), g.getOwnerId());
        putIfChanged(after, "period", req.period(), g.getPeriod());
        putIfChanged(after, "metricUnit", req.metricUnit(), g.getMetricUnit());
        putIfChanged(after, "metricStart", req.metricStart(), g.getMetricStart());
        putIfChanged(after, "metricTarget", req.metricTarget(), g.getMetricTarget());
        if (after.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "拟改值与现值一致，无变更内容");
        }
        // 变更前快照（基线记录）
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("title", g.getTitle());
        before.put("ownerId", g.getOwnerId());
        before.put("period", g.getPeriod());
        before.put("metricUnit", g.getMetricUnit());
        before.put("metricStart", g.getMetricStart());
        before.put("metricTarget", g.getMetricTarget());
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

    private void putIfChanged(Map<String, Object> after, String key, Object proposed, Object current) {
        if (proposed != null && !proposed.toString().equals(current == null ? null : current.toString())) {
            after.put(key, proposed);
        }
    }
}
