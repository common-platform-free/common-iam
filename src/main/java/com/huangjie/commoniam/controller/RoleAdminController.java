package com.huangjie.commoniam.controller;

import com.huangjie.commoniam.common.ApiResult;
import com.huangjie.commoniam.dto.CreateRoleRequest;
import com.huangjie.commoniam.dto.UpdateRoleRequest;
import com.huangjie.commoniam.service.KeycloakRoleService;
import com.huangjie.commoniam.vo.RoleVO;
import jakarta.validation.Valid;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Realm Role 管理接口。
 *
 * <p>第一版只管理 Keycloak Realm Role，不管理 Client Role。</p>
 */
@RestController
@RequestMapping("/iam/admin/roles")
@RequiredArgsConstructor
public class RoleAdminController {

    private final KeycloakRoleService keycloakRoleService;

    /**
     * 创建 Realm Role。
     */
    @PostMapping(value = "/create")
    public ApiResult<Boolean> createRole(@Valid @RequestBody CreateRoleRequest request) {
        keycloakRoleService.createRole(request);
        return ApiResult.success(true);
    }

    /**
     * 查询 Realm Role 列表。
     */
    @GetMapping(value = "/list")
    public ApiResult<List<RoleVO>> listRoles() {
        return ApiResult.success(keycloakRoleService.listRoles());
    }

    /**
     * 查询指定 Realm Role。
     */
    @GetMapping("/{roleName}")
    public ApiResult<RoleVO> getRole(@PathVariable String roleName) {
        return ApiResult.success(keycloakRoleService.getRole(roleName));
    }

    /**
     * 更新 Realm Role。
     */
    @PutMapping("/{roleName}")
    public ApiResult<Boolean> updateRole(@PathVariable String roleName, @Valid @RequestBody UpdateRoleRequest request) {
        keycloakRoleService.updateRole(roleName, request);
        return ApiResult.success(true);
    }

    /**
     * 删除 Realm Role。
     */
    @DeleteMapping("/{roleName}")
    public ApiResult<Boolean> deleteRole(@PathVariable String roleName) {
        keycloakRoleService.deleteRole(roleName);
        return ApiResult.success(true);
    }
}
