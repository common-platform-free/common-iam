package com.huangjie.commoniam.service;

import com.huangjie.commoniam.client.KeycloakUserClient;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Keycloak 用户 Admin API 封装。
 *
 * <p>IAM 对外暴露自己的用户管理接口，内部通过 Keycloak Admin API 操作真实用户数据。
 * 这里不直接操作 Keycloak 数据库，也不会读取用户密码。</p>
 */
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

    private final KeycloakUserClient keycloakUserClient;
    private final KeycloakProperties keycloakProperties;
    private final KeycloakAdminTokenService adminTokenService;

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

        ResponseEntity<Void> response = keycloakUserClient.createUser(bearerAuth(), keycloakProperties.getRealm(), body);
        String userId = parseUserIdFromLocation(response.getHeaders().getLocation());
        return userId == null ? findUserIdByUsername(request.username()) : userId;
    }

    /**
     * 查询用户列表。
     * 对外暴露 page/size，内部转换为 Keycloak 的 first/max 偏移分页。
     */
    public List<UserVO> listUsers(String username, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int first = (safePage - 1) * safeSize;
        List<Map<String, Object>> users = keycloakUserClient.listUsers(
                bearerAuth(),
                keycloakProperties.getRealm(),
                username,
                first,
                safeSize
        );
        return users == null ? List.of() : users.stream().map(this::toUserVO).toList();
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
        keycloakUserClient.resetPassword(bearerAuth(), keycloakProperties.getRealm(), userId, body);
    }

    /**
     * 删除 Keycloak 用户。
     */
    public void deleteUser(String userId) {
        keycloakUserClient.deleteUser(bearerAuth(), keycloakProperties.getRealm(), userId);
    }

    /**
     * 按用户名精确查询单个用户。
     * 登录成功后会用它把 username 转成 Keycloak userId。
     */
    public UserVO findSingleUserByUsername(String username) {
        List<Map<String, Object>> users = keycloakUserClient.findUsersByUsername(
                bearerAuth(),
                keycloakProperties.getRealm(),
                username,
                true,
                0,
                2
        );
        if (users == null || users.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toUserVO(users.get(0));
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
        List<Map<String, Object>> roles = keycloakUserClient.getUserRealmRoles(
                bearerAuth(),
                keycloakProperties.getRealm(),
                userId
        );
        return roles == null ? List.of() : roles.stream().map(this::toRoleVO).toList();
    }

    /**
     * 给用户分配 Realm Role。
     * Keycloak 要求传入完整 role representation，不只是 roleName。
     */
    public void assignRealmRoles(String userId, List<Map<String, Object>> roleRepresentations) {
        keycloakUserClient.assignRealmRoles(bearerAuth(), keycloakProperties.getRealm(), userId, roleRepresentations);
    }

    /**
     * 从用户身上移除 Realm Role。
     * DELETE 请求体同样需要完整 role representation。
     */
    public void removeRealmRoles(String userId, List<Map<String, Object>> roleRepresentations) {
        keycloakUserClient.removeRealmRoles(bearerAuth(), keycloakProperties.getRealm(), userId, roleRepresentations);
    }

    /**
     * 获取 Keycloak 原始用户 representation，供内部转换为 UserVO。
     */
    private Map<String, Object> getUserRepresentation(String userId) {
        return keycloakUserClient.getUser(bearerAuth(), keycloakProperties.getRealm(), userId);
    }

    /**
     * 调用 Keycloak 用户更新接口。
     */
    private void updateUserRepresentation(String userId, Map<String, Object> body) {
        keycloakUserClient.updateUser(bearerAuth(), keycloakProperties.getRealm(), userId, body);
    }

    private String findUserIdByUsername(String username) {
        return findSingleUserByUsername(username).id();
    }

    /**
     * Admin API 统一加上 bearer token。
     */
    private String bearerAuth() {
        return "Bearer " + adminTokenService.getAdminToken();
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
