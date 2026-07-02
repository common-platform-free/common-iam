package com.huangjie.commoniam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 发送模拟验证码请求体。
 *
 * <p>第一版用 username 标识验证码归属，验证码只输出到服务端日志。</p>
 */
public record CaptchaSendRequest(
        @NotBlank(message = "用户名不能为空") String username
) {
}
