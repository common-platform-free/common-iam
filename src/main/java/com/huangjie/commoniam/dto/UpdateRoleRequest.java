package com.huangjie.commoniam.dto;

/**
 * 更新 Realm Role 的请求体。
 */
public record UpdateRoleRequest(
        String name,
        String description
) {
}
