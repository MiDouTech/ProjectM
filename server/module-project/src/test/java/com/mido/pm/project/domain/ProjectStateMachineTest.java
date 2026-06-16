package com.mido.pm.project.domain;

import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static com.mido.pm.project.domain.ProjectStatus.APPROVING;
import static com.mido.pm.project.domain.ProjectStatus.CLOSED;
import static com.mido.pm.project.domain.ProjectStatus.DRAFT;
import static com.mido.pm.project.domain.ProjectStatus.EVALUATED;
import static com.mido.pm.project.domain.ProjectStatus.IN_PROGRESS;
import static com.mido.pm.project.domain.ProjectStatus.REGISTERED;
import static com.mido.pm.project.domain.ProjectStatus.RESULT_VERIFY;
import static com.mido.pm.project.domain.ProjectStatus.VALUE_VERIFY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 项目状态机流转单测：合法流转通过，非法流转被拒（§2.2）。
 */
class ProjectStateMachineTest {

    @Test
    void legalTransitionsPass() {
        assertTrue(ProjectStateMachine.canTransit(DRAFT, APPROVING));
        assertTrue(ProjectStateMachine.canTransit(APPROVING, REGISTERED));
        assertTrue(ProjectStateMachine.canTransit(APPROVING, DRAFT));          // 驳回
        assertTrue(ProjectStateMachine.canTransit(REGISTERED, IN_PROGRESS));
        assertTrue(ProjectStateMachine.canTransit(IN_PROGRESS, RESULT_VERIFY));
        assertTrue(ProjectStateMachine.canTransit(RESULT_VERIFY, CLOSED));
        assertTrue(ProjectStateMachine.canTransit(RESULT_VERIFY, IN_PROGRESS)); // 打回
        assertTrue(ProjectStateMachine.canTransit(CLOSED, VALUE_VERIFY));
        assertTrue(ProjectStateMachine.canTransit(VALUE_VERIFY, EVALUATED));
        assertDoesNotThrow(() -> ProjectStateMachine.assertTransit(DRAFT, APPROVING));
    }

    @Test
    void illegalTransitionsRejected() {
        // 跳步
        assertFalse(ProjectStateMachine.canTransit(DRAFT, IN_PROGRESS));
        assertFalse(ProjectStateMachine.canTransit(DRAFT, REGISTERED));
        assertFalse(ProjectStateMachine.canTransit(REGISTERED, CLOSED));
        assertFalse(ProjectStateMachine.canTransit(IN_PROGRESS, CLOSED));
        // 终态无出边
        assertFalse(ProjectStateMachine.canTransit(EVALUATED, DRAFT));
        // 回退非法
        assertFalse(ProjectStateMachine.canTransit(IN_PROGRESS, REGISTERED));
        // 自环非法
        assertFalse(ProjectStateMachine.canTransit(DRAFT, DRAFT));
    }

    @Test
    void assertTransitThrowsOnIllegal() {
        BizException ex = assertThrows(BizException.class,
                () -> ProjectStateMachine.assertTransit(DRAFT, IN_PROGRESS));
        assertTrue(ex.getMessage().contains("非法状态流转"));
    }
}
