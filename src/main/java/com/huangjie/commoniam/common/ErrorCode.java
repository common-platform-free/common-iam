package com.huangjie.commoniam.common;

import lombok.Getter;

/**
 * 统一错误码。
 *
 * <p>当前 code 直接对齐常见 HTTP 状态码，便于异常处理时同时设置响应状态。</p>
 */
@Getter
public enum ErrorCode {

    SUCCESS(0),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    INTERNAL_ERROR(500);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }
}
