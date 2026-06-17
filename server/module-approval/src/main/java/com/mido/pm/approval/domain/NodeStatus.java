package com.mido.pm.approval.domain;

/** 节点求值结果：待定 / 通过 / 驳回。 */
public enum NodeStatus {
    PENDING,
    PASSED,
    REJECTED
}
