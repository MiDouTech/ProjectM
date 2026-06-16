package com.mido.pm.common.exception;

/**
 * 全局错误码集中登记（遵循 docs/api-conventions.md）。
 * 业务错误码禁止散落硬编码，新增前在此查重登记。
 * 约定：code=0 成功；4xxxx 客户端类；5xxxx 服务端类。httpStatus 为对应 HTTP 状态。
 */
public enum ErrorCode {

    SUCCESS(0, "ok", 200),

    PARAM_ERROR(40000, "参数校验失败", 400),
    UNAUTHORIZED(40100, "未认证或登录已过期", 401),
    FORBIDDEN(40300, "无权限", 403),
    NOT_FOUND(40400, "资源不存在", 404),
    CONFLICT(40900, "资源状态冲突", 409),

    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
