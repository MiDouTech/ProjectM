package com.mido.pm.provider.identity;

/** 企微部门（通讯录同步用）。parentId 为企微父部门 id（根为 1）。 */
public record WecomDept(long id, String name, long parentId) {
}
