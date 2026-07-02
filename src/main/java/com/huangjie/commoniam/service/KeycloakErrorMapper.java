package com.huangjie.commoniam.service;

import com.huangjie.commoniam.common.ErrorCode;
import com.huangjie.commoniam.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * Keycloak HTTP 错误映射器。
 *
 * <p>把 Keycloak REST/Admin API 返回的状态码转换成 IAM 统一异常，
 * 最终由 GlobalExceptionHandler 包装成统一响应体。</p>
 */
@Component
public class KeycloakErrorMapper {

    /**
     * 根据 Keycloak HTTP 状态码返回业务异常。
     */
    public BusinessException toBusinessException(int status, String statusText) {
        return toBusinessException(null, status, statusText);
    }

    /**
     * 根据 Feign 方法和 Keycloak HTTP 状态码返回业务异常。
     *
     * <p>Keycloak token 端点在用户名/密码错误、grant 参数错误时常返回 400。
     * 对登录接口而言，前端不应该看到底层 Bad Request，而应该看到可理解的登录失败提示。</p>
     */
    public BusinessException toBusinessException(String methodKey, int status, String statusText) {
        if (isTokenEndpoint(methodKey) && (status == 400 || status == 401)) {
            return new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        return switch (status) {
            case 401 -> new BusinessException(ErrorCode.UNAUTHORIZED, "Keycloak client 配置错误或登录失败");
            case 403 -> new BusinessException(ErrorCode.FORBIDDEN, "admin client 没有权限");
            case 404 -> new BusinessException(ErrorCode.NOT_FOUND, "realm、userId 或 roleName 不存在");
            case 409 -> new BusinessException(ErrorCode.CONFLICT, "用户或角色已存在");
            default -> new BusinessException(ErrorCode.INTERNAL_ERROR, "Keycloak 调用失败: " + statusText);
        };
    }

    private boolean isTokenEndpoint(String methodKey) {
        return methodKey != null && methodKey.contains("KeycloakTokenClient#token");
    }
}
