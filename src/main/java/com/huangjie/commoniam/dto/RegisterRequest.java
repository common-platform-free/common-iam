package com.huangjie.commoniam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 普通用户自助注册请求体。
 *
 * <p>注册接口只暴露必要字段，不允许前端直接传姓名、enabled、attributes 或 roles。</p>
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        @Email(message = "邮箱格式不正确") String email
) {
}
