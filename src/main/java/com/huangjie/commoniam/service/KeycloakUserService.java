package com.huangjie.commoniam.service;

import com.huangjie.commoniam.common.ErrorCode;
import com.huangjie.commoniam.config.KeycloakProperties;
import com.huangjie.commoniam.dto.CreateUserRequest;
import com.huangjie.commoniam.dto.ResetPasswordRequest;
import com.huangjie.commoniam.dto.UpdateUserRequest;
import com.huangjie.commoniam.exception.BusinessException;
import com.huangjie.commoniam.vo.RoleVO;
import com.huangjie.commoniam.vo.UserVO;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Keycloak 用户 Admin API 封装。
 *
 * <p>IAM 对外暴露自己的用户管理接口，内部通过 Keycloak Admin API 操作真实用户数据。
 * 这里不直接操作 Keycloak 数据库，也不会读取用户密码。</p>
 */
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAP =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient keycloakRestClient;
    private final KeycloakProperties keycloakProperties;
    private final KeycloakAdminTokenService adminTokenService;
    private final KeycloakErrorMapper keycloakErrorMapper;

    /**
     * 创建 Keycloak 用户，并设置初始密码。
     * Keycloak 创建成功通常会在 Location header 中返回用户 ID。
     */
    public String createUser(CreateUserRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", request.username());
        body.put("email", request.email());
        body.put("firstName", request.firstName());
        body.put("lastName", request.lastName());
        body.put("enabled", request.enabled() == null || request.enabled());
        body.put("attributes", request.attributes());
        body.put("credentials", List.of(Map.of(
                "type", "password",
                "value", request.password(),
                "temporary", false
        )));

        try {
            ResponseEntity<Void> response = keycloakRestClient.post()
                    .uri("/admin/realms/{realm}/users", keycloakProperties.getRealm())
                    .headers(this::bearerAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            String userId = parseUserIdFromLocation(response.getHeaders().getLocation());
            return userId == null ? findUserIdByUsername(request.username()) : userId;
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }

    /**
     * 查询用户列表。
     * 对外暴露 page/size，内部转换为 Keycloak 的 first/max 偏移分页。
     */
    public List<UserVO> listUsers(String username, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int first = (safePage - 1) * safeSize;
        try {
            List<Map<String, Object>> users = keycloakRestClient.get()
                    .uri(builder -> builder
                            .path("/admin/realms/{realm}/users")
                            .queryParamIfPresent("username", java.util.Optional.ofNullable(username))
                            .queryParam("first", first)
                            .queryParam("max", safeSize)
                            .build(keycloakProperties.getRealm()))
                    .headers(this::bearerAuth)
                    .retrieve()
                    .body(LIST_OF_MAP);
            return users == null ? List.of() : users.stream().map(this::toUserVO).toList();
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }

    /**
     * 根据 Keycloak userId 查询用户详情。
     */
    public UserVO getUser(String userId) {
        return toUserVO(getUserRepresentation(userId));
    }

    /**
     * 更新用户基础资料。
     * 只把请求体中非 null 的字段传给 Keycloak，避免误清空未传字段。
     */
    public void updateUser(String userId, UpdateUserRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        putIfNotNull(body, "email", request.email());
        putIfNotNull(body, "firstName", request.firstName());
        putIfNotNull(body, "lastName", request.lastName());
        putIfNotNull(body, "enabled", request.enabled());
        putIfNotNull(body, "attributes", request.attributes());
        updateUserRepresentation(userId, body);
    }

    /**
     * 启用用户。
     */
    public void enableUser(String userId) {
        updateUserRepresentation(userId, Map.of("enabled", true));
    }

    /**
     * 禁用用户。
     */
    public void disableUser(String userId) {
        updateUserRepresentation(userId, Map.of("enabled", false));
    }

    /**
     * 重置用户密码。
     * temporary=true 时，Keycloak 会要求用户下次登录后修改密码。
     */
    public void resetPassword(String userId, ResetPasswordRequest request) {
        Map<String, Object> body = Map.of(
                "type", "password",
                "value", request.password(),
                "temporary", request.temporary() != null && request.temporary()
        );
        try {
            keycloakRestClient.put()
                    .uri("/admin/realms/{realm}/users/{userId}/reset-password", keycloakProperties.getRealm(), userId)
                    .headers(this::bearerAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }

    /**
     * 删除 Keycloak 用户。
     */
    public void deleteUser(String userId) {
        try {
            keycloakRestClient.delete()
                    .uri("/admin/realms/{realm}/users/{userId}", keycloakProperties.getRealm(), userId)
                    .headers(this::bearerAuth)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }

    /**
     * 按用户名精确查询单个用户。
     * 登录成功后会用它把 username 转成 Keycloak userId。
     */
    public UserVO findSingleUserByUsername(String username) {
        try {
            List<Map<String, Object>> users = keycloakRestClient.get()
                    .uri(builder -> builder
                            .path("/admin/realms/{realm}/users")
                            .queryParam("username", username)
                            .queryParam("exact", true)
                            .queryParam("first", 0)
                            .queryParam("max", 2)
                            .build(keycloakProperties.getRealm()))
                    .headers(this::bearerAuth)
                    .retrieve()
                    .body(LIST_OF_MAP);
            if (users == null || users.isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
            }
            return toUserVO(users.get(0));
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }

    /**
     * 刷新 token 前确认用户仍然存在且未被禁用。
     */
    public void assertUserEnabled(String userId) {
        UserVO user = getUser(userId);
        if (!Boolean.TRUE.equals(user.enabled())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户已被禁用");
        }
    }

    /**
     * 查询用户当前 Realm Role 名称。
     * access_token 的 roles claim 始终使用 Keycloak 中的最新角色。
     */
    public List<String> getUserRealmRoleNames(String userId) {
        return getUserRealmRoles(userId).stream()
                .map(RoleVO::name)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 查询用户当前拥有的 Realm Role。
     */
    public List<RoleVO> getUserRealmRoles(String userId) {
        try {
            List<Map<String, Object>> roles = keycloakRestClient.get()
                    .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", keycloakProperties.getRealm(), userId)
                    .headers(this::bearerAuth)
                    .retrieve()
                    .body(LIST_OF_MAP);
            return roles == null ? List.of() : roles.stream().map(this::toRoleVO).toList();
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }

    /**
     * 给用户分配 Realm Role。
     * Keycloak 要求传入完整 role representation，不只是 roleName。
     */
    public void assignRealmRoles(String userId, List<Map<String, Object>> roleRepresentations) {
        try {
            keycloakRestClient.post()
                    .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", keycloakProperties.getRealm(), userId)
                    .headers(this::bearerAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(roleRepresentations)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }

    /**
     * 从用户身上移除 Realm Role。
     * DELETE 请求体同样需要完整 role representation。
     */
    public void removeRealmRoles(String userId, List<Map<String, Object>> roleRepresentations) {
        try {
            keycloakRestClient.method(HttpMethod.DELETE)
                    .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", keycloakProperties.getRealm(), userId)
                    .headers(this::bearerAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(roleRepresentations)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }

    /**
     * 获取 Keycloak 原始用户 representation，供内部转换为 UserVO。
     */
    private Map<String, Object> getUserRepresentation(String userId) {
        try {
            return keycloakRestClient.get()
                    .uri("/admin/realms/{realm}/users/{userId}", keycloakProperties.getRealm(), userId)
                    .headers(this::bearerAuth)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }

    /**
     * 调用 Keycloak 用户更新接口。
     */
    private void updateUserRepresentation(String userId, Map<String, Object> body) {
        try {
            keycloakRestClient.put()
                    .uri("/admin/realms/{realm}/users/{userId}", keycloakProperties.getRealm(), userId)
                    .headers(this::bearerAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }

    private String findUserIdByUsername(String username) {
        return findSingleUserByUsername(username).id();
    }

    /**
     * Admin API 统一加上 bearer token。
     */
    private void bearerAuth(HttpHeaders headers) {
        headers.setBearerAuth(adminTokenService.getAdminToken());
    }

    private void putIfNotNull(Map<String, Object> body, String key, Object value) {
        if (value != null) {
            body.put(key, value);
        }
    }

    /**
     * 从 Keycloak 创建用户响应的 Location header 中提取 userId。
     */
    private String parseUserIdFromLocation(URI location) {
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        int index = path.lastIndexOf('/');
        return index >= 0 ? path.substring(index + 1) : null;
    }

    /**
     * 将 Keycloak 的用户 Map 转换为对外返回的 VO。
     */
    private UserVO toUserVO(Map<String, Object> user) {
        return new UserVO(
                stringValue(user.get("id")),
                stringValue(user.get("username")),
                stringValue(user.get("email")),
                stringValue(user.get("firstName")),
                stringValue(user.get("lastName")),
                (Boolean) user.get("enabled"),
                attributesValue(user.get("attributes"))
        );
    }

    private RoleVO toRoleVO(Map<String, Object> role) {
        return new RoleVO(
                stringValue(role.get("id")),
                stringValue(role.get("name")),
                stringValue(role.get("description"))
        );
    }

    @SuppressWarnings("unchecked")
    /**
     * Keycloak attributes 的值通常是 List<String>，这里做兼容转换。
     */
    private Map<String, List<String>> attributesValue(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, List<String>> attributes = new LinkedHashMap<>();
        map.forEach((key, attrValue) -> attributes.put(String.valueOf(key), listValue(attrValue)));
        return attributes;
    }

    private List<String> listValue(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        values.add(String.valueOf(value));
        return values;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
