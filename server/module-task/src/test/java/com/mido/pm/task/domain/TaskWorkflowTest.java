package com.mido.pm.task.domain;

import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static com.mido.pm.task.domain.TaskStatus.ACCEPTED;
import static com.mido.pm.task.domain.TaskStatus.DONE;
import static com.mido.pm.task.domain.TaskStatus.IN_PROGRESS;
import static com.mido.pm.task.domain.TaskStatus.NOT_STARTED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 任务默认工作流流转单测：合法通过，非法被拒。
 */
class TaskWorkflowTest {

    @Test
    void legalTransitions() {
        assertTrue(TaskWorkflow.canTransit(NOT_STARTED, IN_PROGRESS));
        assertTrue(TaskWorkflow.canTransit(IN_PROGRESS, DONE));
        assertTrue(TaskWorkflow.canTransit(IN_PROGRESS, NOT_STARTED));
        assertTrue(TaskWorkflow.canTransit(DONE, IN_PROGRESS));      // 打回
        assertTrue(TaskWorkflow.canTransit(DONE, ACCEPTED));
        assertTrue(TaskWorkflow.canTransit(ACCEPTED, DONE));         // 验收打回
    }

    @Test
    void illegalTransitions() {
        assertFalse(TaskWorkflow.canTransit(NOT_STARTED, DONE));
        assertFalse(TaskWorkflow.canTransit(NOT_STARTED, ACCEPTED));
        assertFalse(TaskWorkflow.canTransit(IN_PROGRESS, ACCEPTED));
        assertFalse(TaskWorkflow.canTransit(ACCEPTED, NOT_STARTED));
        assertFalse(TaskWorkflow.canTransit(NOT_STARTED, NOT_STARTED));
    }

    @Test
    void assertThrowsOnIllegal() {
        assertThrows(BizException.class, () -> TaskWorkflow.assertTransit(NOT_STARTED, ACCEPTED));
    }
}
