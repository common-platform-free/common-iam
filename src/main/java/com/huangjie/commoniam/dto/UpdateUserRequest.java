package com.huangjie.commoniam.dto;

import java.util.List;
import java.util.Map;

/**
 * 更新 Keycloak 用户资料的请求体。
 *
 * <p>字段为 null 时不会传给 Keycloak，避免误覆盖。</p>
 */
public record UpdateUserRequest(
        String email,
        String firstName,
        String lastName,
        Boolean enabled,
        Map<String, List<String>> attributes
) {
}
