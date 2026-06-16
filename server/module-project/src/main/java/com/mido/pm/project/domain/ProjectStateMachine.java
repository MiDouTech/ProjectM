package com.mido.pm.project.domain;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.mido.pm.project.domain.ProjectStatus.APPROVING;
import static com.mido.pm.project.domain.ProjectStatus.CLOSED;
import static com.mido.pm.project.domain.ProjectStatus.DRAFT;
import static com.mido.pm.project.domain.ProjectStatus.EVALUATED;
import static com.mido.pm.project.domain.ProjectStatus.IN_PROGRESS;
import static com.mido.pm.project.domain.ProjectStatus.REGISTERED;
import static com.mido.pm.project.domain.ProjectStatus.RESULT_VERIFY;
import static com.mido.pm.project.domain.ProjectStatus.VALUE_VERIFY;

/**
 * 项目生命周期状态机（architecture-overview §2.2）。封装合法流转，非法流转拒绝。
 * 纯逻辑、无副作用；职级/审批结果 guard 在服务层于流转前后施加。
 */
public final class ProjectStateMachine {

    /** 合法流转表：from → 允许的 to 集合。 */
    private static final Map<ProjectStatus, Set<ProjectStatus>> TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(APPROVING),
            APPROVING, EnumSet.of(REGISTERED, DRAFT),       // 通过→已注册 / 驳回→草稿
            REGISTERED, EnumSet.of(IN_PROGRESS),
            IN_PROGRESS, EnumSet.of(RESULT_VERIFY),
            RESULT_VERIFY, EnumSet.of(CLOSED, IN_PROGRESS), // 达标→已结案 / 打回→进行中
            CLOSED, EnumSet.of(VALUE_VERIFY),               // 定时唤醒
            VALUE_VERIFY, EnumSet.of(EVALUATED),
            EVALUATED, EnumSet.noneOf(ProjectStatus.class)
    );

    private ProjectStateMachine() {
    }

    public static boolean canTransit(ProjectStatus from, ProjectStatus to) {
        return from != null && to != null
                && TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /** 校验流转合法，非法抛 {@link BizException}(409)。 */
    public static void assertTransit(ProjectStatus from, ProjectStatus to) {
        if (!canTransit(from, to)) {
            String f = from == null ? "?" : from.getCode();
            String t = to == null ? "?" : to.getCode();
            throw new BizException(ErrorCode.CONFLICT, "非法状态流转：" + f + " → " + t);
        }
    }
}
