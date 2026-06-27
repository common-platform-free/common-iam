package com.huangjie.commoniam.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 创建 Keycloak 用户的请求体。
 *
 * <p>password 只用于调用 Keycloak 创建 credential，IAM 不保存密码。</p>
 */
public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        String email,
        String firstName,
        String lastName,
        Boolean enabled,
        Map<String, List<String>> attributes
) {
}
