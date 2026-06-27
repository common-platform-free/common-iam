package com.huangjie.commoniam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 重置用户密码的请求体。
 */
public record ResetPasswordRequest(
        @NotBlank(message = "密码不能为空") String password,
        Boolean temporary
) {
}
