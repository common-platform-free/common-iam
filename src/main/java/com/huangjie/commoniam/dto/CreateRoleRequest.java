package com.huangjie.commoniam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建 Realm Role 的请求体。
 */
public record CreateRoleRequest(
        @NotBlank(message = "角色名称不能为空") String name,
        String description
) {
}
