package com.huangjie.commoniam.client;

import com.huangjie.commoniam.config.KeycloakFeignConfig;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Keycloak Realm Role Admin API。
 *
 * <p>第一版 IAM 只访问 Realm Role 相关接口，不访问 Client Role 接口。</p>
 */
@FeignClient(name = "keycloak-role-client", url = "${keycloak.server-url}", configuration = KeycloakFeignConfig.class)
public interface KeycloakRoleClient {

    /**
     * 创建 Realm Role。
     *
     * <p>路径：POST /admin/realms/{realm}/roles</p>
     */
    @PostMapping(value = "/admin/realms/{realm}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
    void createRole(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @RequestBody Map<String, Object> body
    );

    /**
     * 查询 Realm Role 列表。
     *
     * <p>路径：GET /admin/realms/{realm}/roles</p>
     */
    @GetMapping("/admin/realms/{realm}/roles")
    List<Map<String, Object>> listRoles(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm
    );

    /**
     * 查询指定 Realm Role。
     *
     * <p>路径：GET /admin/realms/{realm}/roles/{roleName}</p>
     *
     * <p>用户角色分配前会先通过该接口拿到完整 role representation。</p>
     */
    @GetMapping("/admin/realms/{realm}/roles/{roleName}")
    Map<String, Object> getRole(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @PathVariable("roleName") String roleName
    );

    /**
     * 更新 Realm Role。
     *
     * <p>路径：PUT /admin/realms/{realm}/roles/{roleName}</p>
     */
    @PutMapping(value = "/admin/realms/{realm}/roles/{roleName}", consumes = MediaType.APPLICATION_JSON_VALUE)
    void updateRole(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @PathVariable("roleName") String roleName,
            @RequestBody Map<String, Object> body
    );

    /**
     * 删除 Realm Role。
     *
     * <p>路径：DELETE /admin/realms/{realm}/roles/{roleName}</p>
     */
    @DeleteMapping("/admin/realms/{realm}/roles/{roleName}")
    void deleteRole(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @PathVariable("roleName") String roleName
    );
}
