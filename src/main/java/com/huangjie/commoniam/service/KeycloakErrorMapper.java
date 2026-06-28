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
        return switch (status) {
            case 401 -> new BusinessException(ErrorCode.UNAUTHORIZED, "Keycloak client 配置错误或登录失败");
            case 403 -> new BusinessException(ErrorCode.FORBIDDEN, "admin client 没有权限");
            case 404 -> new BusinessException(ErrorCode.NOT_FOUND, "realm、userId 或 roleName 不存在");
            case 409 -> new BusinessException(ErrorCode.CONFLICT, "用户或角色已存在");
            default -> new BusinessException(ErrorCode.INTERNAL_ERROR, "Keycloak 调用失败: " + statusText);
        };
    }
}
