package com.huangjie.commoniam.service;

import com.huangjie.commoniam.client.KeycloakRoleClient;
import com.huangjie.commoniam.common.ErrorCode;
import com.huangjie.commoniam.config.KeycloakProperties;
import com.huangjie.commoniam.dto.CreateRoleRequest;
import com.huangjie.commoniam.dto.UpdateRoleRequest;
import com.huangjie.commoniam.exception.BusinessException;
import com.huangjie.commoniam.vo.RoleVO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Keycloak Realm Role Admin API 封装。
 *
 * <p>第一版 IAM 只使用 Realm Role，不使用 Client Role。
 * 角色的创建、查询、修改、删除都委托给 Keycloak。</p>
 */
@Service
@RequiredArgsConstructor
public class KeycloakRoleService {

    private final KeycloakRoleClient keycloakRoleClient;
    private final KeycloakProperties keycloakProperties;
    private final KeycloakAdminTokenService adminTokenService;

    /**
     * 创建 Realm Role。
     */
    public void createRole(CreateRoleRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", request.name());
        body.put("description", request.description());
        keycloakRoleClient.createRole(bearerAuth(), keycloakProperties.getRealm(), body);
    }

    /**
     * 查询 Realm Role 列表。
     */
    public List<RoleVO> listRoles() {
        List<Map<String, Object>> roles = keycloakRoleClient.listRoles(bearerAuth(), keycloakProperties.getRealm());
        return roles == null ? List.of() : roles.stream().map(this::toRoleVO).toList();
    }

    /**
     * 根据角色名查询 Realm Role。
     */
    public RoleVO getRole(String roleName) {
        return toRoleVO(getRoleRepresentation(roleName));
    }

    /**
     * 更新 Realm Role。
     */
    public void updateRole(String roleName, UpdateRoleRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.name() != null) {
            body.put("name", request.name());
        }
        if (request.description() != null) {
            body.put("description", request.description());
        }
        keycloakRoleClient.updateRole(bearerAuth(), keycloakProperties.getRealm(), roleName, body);
    }

    /**
     * 删除 Realm Role。
     */
    public void deleteRole(String roleName) {
        keycloakRoleClient.deleteRole(bearerAuth(), keycloakProperties.getRealm(), roleName);
    }

    /**
     * 批量查询 Keycloak role representation。
     * 用户角色映射接口需要这个原始结构。
     */
    public List<Map<String, Object>> getRoleRepresentations(List<String> roleNames) {
        return roleNames.stream().map(this::getRoleRepresentation).toList();
    }

    /**
     * 确保 Realm Role 存在，并返回 Keycloak 原始 role representation。
     *
     * <p>注册默认角色这类系统内置角色不能假设 Keycloak 已经提前创建。
     * 如果查询不到，则先创建；如果并发创建时遇到 409，再重新查询即可。</p>
     */
    public Map<String, Object> ensureRoleRepresentation(String roleName, String description) {
        try {
            return getRoleRepresentation(roleName);
        } catch (BusinessException ex) {
            if (ex.getErrorCode() != ErrorCode.NOT_FOUND) {
                throw ex;
            }
        }

        try {
            createRole(new CreateRoleRequest(roleName, description));
        } catch (BusinessException ex) {
            if (ex.getErrorCode() != ErrorCode.CONFLICT) {
                throw ex;
            }
        }
        return getRoleRepresentation(roleName);
    }

    /**
     * 查询 Keycloak 原始 role representation。
     */
    public Map<String, Object> getRoleRepresentation(String roleName) {
        return keycloakRoleClient.getRole(bearerAuth(), keycloakProperties.getRealm(), roleName);
    }

    /**
     * Admin API 统一加上 bearer token。
     */
    private String bearerAuth() {
        return "Bearer " + adminTokenService.getAdminToken();
    }

    /**
     * 将 Keycloak 的角色 Map 转成对外 VO。
     */
    private RoleVO toRoleVO(Map<String, Object> role) {
        return new RoleVO(
                stringValue(role.get("id")),
                stringValue(role.get("name")),
                stringValue(role.get("description"))
        );
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
