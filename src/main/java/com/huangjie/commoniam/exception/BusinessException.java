package com.huangjie.commoniam.exception;

import com.huangjie.commoniam.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>Service 层遇到可预期错误时抛出该异常，
 * 由 GlobalExceptionHandler 转换成统一 API 响应。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
