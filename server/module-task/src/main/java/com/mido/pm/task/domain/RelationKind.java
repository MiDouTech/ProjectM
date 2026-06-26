package com.mido.pm.task.domain;

import java.util.Set;

/**
 * 工作项关联类型：related 相关(横向，如 任务↔缺陷) / derived 派生(纵向父子)。
 */
public final class RelationKind {

    public static final String RELATED = "related";
    public static final String DERIVED = "derived";

    private static final Set<String> ALL = Set.of(RELATED, DERIVED);

    public static boolean isValid(String kind) {
        return ALL.contains(kind);
    }

    private RelationKind() {
    }
}
