package com.huangjie.commoniam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 验证码登录请求体。
 */
public record CaptchaLoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "验证码不能为空") String code
) {
}
