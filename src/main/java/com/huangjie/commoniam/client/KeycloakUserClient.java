package com.huangjie.commoniam.client;

import com.huangjie.commoniam.config.KeycloakFeignConfig;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Keycloak User Admin API。
 *
 * <p>这个接口集中展示 IAM 会访问的 Keycloak 用户相关外部 HTTP 接口。</p>
 */
@FeignClient(name = "keycloak-user-client", url = "${keycloak.server-url}", configuration = KeycloakFeignConfig.class)
public interface KeycloakUserClient {

    /**
     * 创建 Keycloak 用户。
     *
     * <p>路径：POST /admin/realms/{realm}/users</p>
     *
     * <p>创建成功时 Keycloak 通常会通过 Location header 返回新用户 ID。</p>
     */
    @PostMapping(value = "/admin/realms/{realm}/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> createUser(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @RequestBody Map<String, Object> body
    );

    /**
     * 查询用户列表。
     *
     * <p>路径：GET /admin/realms/{realm}/users</p>
     *
     * <p>Keycloak 使用 first/max 偏移分页；service 层负责把 page/size 转成 first/max。</p>
     */
    @GetMapping("/admin/realms/{realm}/users")
    List<Map<String, Object>> listUsers(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam("first") int first,
            @RequestParam("max") int max
    );

    /**
     * 按用户名查询用户。
     *
     * <p>路径：GET /admin/realms/{realm}/users</p>
     *
     * <p>登录成功后，IAM 用这个接口把 username 转换成 Keycloak userId。</p>
     */
    @GetMapping("/admin/realms/{realm}/users")
    List<Map<String, Object>> findUsersByUsername(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @RequestParam("username") String username,
            @RequestParam("exact") boolean exact,
            @RequestParam("first") int first,
            @RequestParam("max") int max
    );

    /**
     * 查询用户详情。
     *
     * <p>路径：GET /admin/realms/{realm}/users/{userId}</p>
     */
    @GetMapping("/admin/realms/{realm}/users/{userId}")
    Map<String, Object> getUser(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @PathVariable("userId") String userId
    );

    /**
     * 更新用户基础资料或启用/禁用状态。
     *
     * <p>路径：PUT /admin/realms/{realm}/users/{userId}</p>
     */
    @PutMapping(value = "/admin/realms/{realm}/users/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    void updateUser(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @PathVariable("userId") String userId,
            @RequestBody Map<String, Object> body
    );

    /**
     * 重置用户密码。
     *
     * <p>路径：PUT /admin/realms/{realm}/users/{userId}/reset-password</p>
     */
    @PutMapping(value = "/admin/realms/{realm}/users/{userId}/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    void resetPassword(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @PathVariable("userId") String userId,
            @RequestBody Map<String, Object> body
    );

    /**
     * 删除用户。
     *
     * <p>路径：DELETE /admin/realms/{realm}/users/{userId}</p>
     */
    @DeleteMapping("/admin/realms/{realm}/users/{userId}")
    void deleteUser(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @PathVariable("userId") String userId
    );

    /**
     * 查询用户已拥有的 Realm Role 映射。
     *
     * <p>路径：GET /admin/realms/{realm}/users/{userId}/role-mappings/realm</p>
     */
    @GetMapping("/admin/realms/{realm}/users/{userId}/role-mappings/realm")
    List<Map<String, Object>> getUserRealmRoles(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @PathVariable("userId") String userId
    );

    /**
     * 给用户分配 Realm Role。
     *
     * <p>路径：POST /admin/realms/{realm}/users/{userId}/role-mappings/realm</p>
     *
     * <p>Keycloak 要求请求体是 role representation 列表，不只是 roleName。</p>
     */
    @PostMapping(value = "/admin/realms/{realm}/users/{userId}/role-mappings/realm",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    void assignRealmRoles(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @PathVariable("userId") String userId,
            @RequestBody List<Map<String, Object>> roleRepresentations
    );

    /**
     * 移除用户的 Realm Role。
     *
     * <p>路径：DELETE /admin/realms/{realm}/users/{userId}/role-mappings/realm</p>
     *
     * <p>Keycloak 的删除角色映射接口也需要请求体携带 role representation 列表。</p>
     */
    @DeleteMapping(value = "/admin/realms/{realm}/users/{userId}/role-mappings/realm",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    void removeRealmRoles(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @PathVariable("userId") String userId,
            @RequestBody List<Map<String, Object>> roleRepresentations
    );
}
