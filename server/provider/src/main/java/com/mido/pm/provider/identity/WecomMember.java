package com.mido.pm.provider.identity;

import java.util.List;

/** 企微成员（通讯录同步用）。userId 为企微 userid；departmentIds 为所属企微部门 id 列表。 */
public record WecomMember(String userId, String name, String mobile, List<Long> departmentIds) {
}
