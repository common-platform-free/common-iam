package com.huangjie.commoniam.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应结构。
 *
 * <p>所有 Controller 成功和失败响应都使用 code/message/data，
 * 方便前端和 Gateway 侧统一处理。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;

    /**
     * 成功响应，code 固定为 0。
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(ErrorCode.SUCCESS.getCode(), "success", data);
    }

    /**
     * 使用系统错误码构造失败响应。
     */
    public static <T> ApiResult<T> error(ErrorCode errorCode, String message) {
        return new ApiResult<>(errorCode.getCode(), message, null);
    }

    /**
     * 使用自定义 code 构造失败响应。
     */
    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
