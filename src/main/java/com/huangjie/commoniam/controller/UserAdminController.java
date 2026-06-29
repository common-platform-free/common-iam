package com.huangjie.commoniam.controller;

import com.huangjie.commoniam.common.ApiResult;
import com.huangjie.commoniam.dto.AssignRoleRequest;
import com.huangjie.commoniam.dto.CreateUserRequest;
import com.huangjie.commoniam.dto.RemoveRoleRequest;
import com.huangjie.commoniam.dto.ResetPasswordRequest;
import com.huangjie.commoniam.dto.UpdateUserRequest;
import com.huangjie.commoniam.service.KeycloakRoleService;
import com.huangjie.commoniam.service.KeycloakUserService;
import com.huangjie.commoniam.vo.RoleVO;
import com.huangjie.commoniam.vo.UserVO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口。
 *
 * <p>接口对外属于 IAM，真实用户数据存储在 Keycloak。
 * 第一版 /iam/admin/** 由 Gateway 负责鉴权，IAM 服务本身不校验 access_token。</p>
 */
@RestController
@RequestMapping("/iam/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final KeycloakUserService keycloakUserService;
    private final KeycloakRoleService keycloakRoleService;

    /**
     * 创建用户，返回 Keycloak userId。
     */
    @PostMapping(value = "/create")
    public ApiResult<String> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResult.success(keycloakUserService.createUser(request));
    }

    /**
     * 查询用户列表。
     * page/size 会在 service 内转换成 Keycloak first/max。
     */
    @GetMapping(value = "/list")
    public ApiResult<List<UserVO>> listUsers(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResult.success(keycloakUserService.listUsers(username, page, size));
    }

    /**
     * 查询用户详情。
     */
    @GetMapping("/{userId}")
    public ApiResult<UserVO> getUser(@PathVariable String userId) {
        return ApiResult.success(keycloakUserService.getUser(userId));
    }

    /**
     * 更新用户资料。
     */
    @PutMapping("/{userId}")
    public ApiResult<Boolean> updateUser(@PathVariable String userId, @Valid @RequestBody UpdateUserRequest request) {
        keycloakUserService.updateUser(userId, request);
        return ApiResult.success(true);
    }

    /**
     * 启用用户。
     */
    @PutMapping("/{userId}/enable")
    public ApiResult<Boolean> enableUser(@PathVariable String userId) {
        keycloakUserService.enableUser(userId);
        return ApiResult.success(true);
    }

    /**
     * 禁用用户。
     */
    @PutMapping("/{userId}/disable")
    public ApiResult<Boolean> disableUser(@PathVariable String userId) {
        keycloakUserService.disableUser(userId);
        return ApiResult.success(true);
    }

    /**
     * 重置用户密码。
     */
    @PutMapping("/{userId}/reset-password")
    public ApiResult<Boolean> resetPassword(@PathVariable String userId, @Valid @RequestBody ResetPasswordRequest request) {
        keycloakUserService.resetPassword(userId, request);
        return ApiResult.success(true);
    }

    /**
     * 删除用户。
     */
    @DeleteMapping("/{userId}")
    public ApiResult<Boolean> deleteUser(@PathVariable String userId) {
        keycloakUserService.deleteUser(userId);
        return ApiResult.success(true);
    }

    /**
     * 给用户分配 Realm Role。
     */
    @PostMapping("/{userId}/roles")
    public ApiResult<Boolean> assignRoles(@PathVariable String userId, @Valid @RequestBody AssignRoleRequest request) {
        List<Map<String, Object>> roles = keycloakRoleService.getRoleRepresentations(request.roleNames());
        keycloakUserService.assignRealmRoles(userId, roles);
        return ApiResult.success(true);
    }

    /**
     * 查询用户已拥有的 Realm Role。
     */
    @GetMapping("/{userId}/roles")
    public ApiResult<List<RoleVO>> getUserRoles(@PathVariable String userId) {
        return ApiResult.success(keycloakUserService.getUserRealmRoles(userId));
    }

    /**
     * 移除用户的 Realm Role。
     */
    @DeleteMapping("/{userId}/roles")
    public ApiResult<Boolean> removeRoles(@PathVariable String userId, @Valid @RequestBody RemoveRoleRequest request) {
        List<Map<String, Object>> roles = keycloakRoleService.getRoleRepresentations(request.roleNames());
        keycloakUserService.removeRealmRoles(userId, roles);
        return ApiResult.success(true);
    }
}
