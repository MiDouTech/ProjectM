package com.mido.pm.platform.dto;

import java.util.List;

/** 当前登录运营人员信息（含权限码，供前端按钮级控制）。 */
public record PlatformMeVO(Long adminId, String username, String name, List<String> perms) {
}
