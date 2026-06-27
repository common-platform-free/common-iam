package com.huangjie.commoniam.vo;

import java.util.List;
import java.util.Map;

/**
 * Keycloak 用户返回对象。
 */
public record UserVO(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        Boolean enabled,
        Map<String, List<String>> attributes
) {
}
