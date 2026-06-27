package com.huangjie.commoniam.service;

import com.huangjie.commoniam.config.KeycloakProperties;
import com.huangjie.commoniam.dto.CreateRoleRequest;
import com.huangjie.commoniam.dto.UpdateRoleRequest;
import com.huangjie.commoniam.vo.RoleVO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Keycloak Realm Role Admin API 封装。
 *
 * <p>第一版 IAM 只使用 Realm Role，不使用 Client Role。
 * 角色的创建、查询、修改、删除都委托给 Keycloak。</p>
 */
@Service
@RequiredArgsConstructor
public class KeycloakRoleService {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAP =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient keycloakRestClient;
    private final KeycloakProperties keycloakProperties;
    private final KeycloakAdminTokenService adminTokenService;
    private final KeycloakErrorMapper keycloakErrorMapper;

    /**
     * 创建 Realm Role。
     */
    public void createRole(CreateRoleRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", request.name());
        body.put("description", request.description());
        try {
            keycloakRestClient.post()
                    .uri("/admin/realms/{realm}/roles", keycloakProperties.getRealm())
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
     * 查询 Realm Role 列表。
     */
    public List<RoleVO> listRoles() {
        try {
            List<Map<String, Object>> roles = keycloakRestClient.get()
                    .uri("/admin/realms/{realm}/roles", keycloakProperties.getRealm())
                    .headers(this::bearerAuth)
                    .retrieve()
                    .body(LIST_OF_MAP);
            return roles == null ? List.of() : roles.stream().map(this::toRoleVO).toList();
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
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
        try {
            keycloakRestClient.put()
                    .uri("/admin/realms/{realm}/roles/{roleName}", keycloakProperties.getRealm(), roleName)
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
     * 删除 Realm Role。
     */
    public void deleteRole(String roleName) {
        try {
            keycloakRestClient.delete()
                    .uri("/admin/realms/{realm}/roles/{roleName}", keycloakProperties.getRealm(), roleName)
                    .headers(this::bearerAuth)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }

    /**
     * 批量查询 Keycloak role representation。
     * 用户角色映射接口需要这个原始结构。
     */
    public List<Map<String, Object>> getRoleRepresentations(List<String> roleNames) {
        return roleNames.stream().map(this::getRoleRepresentation).toList();
    }

    /**
     * 查询 Keycloak 原始 role representation。
     */
    public Map<String, Object> getRoleRepresentation(String roleName) {
        try {
            return keycloakRestClient.get()
                    .uri("/admin/realms/{realm}/roles/{roleName}", keycloakProperties.getRealm(), roleName)
                    .headers(this::bearerAuth)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }

    /**
     * Admin API 统一加上 bearer token。
     */
    private void bearerAuth(HttpHeaders headers) {
        headers.setBearerAuth(adminTokenService.getAdminToken());
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
