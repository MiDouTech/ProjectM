package com.mido.pm.org.dto;

/** 企微通讯录同步结果。 */
public record WecomSyncResultVO(
        int deptCount,
        int userCreated,
        int userUpdated) {
}
