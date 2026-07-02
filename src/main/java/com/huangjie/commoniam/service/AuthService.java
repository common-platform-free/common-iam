package com.huangjie.commoniam.service;

import com.huangjie.commoniam.common.ErrorCode;
import com.huangjie.commoniam.config.AuthCookieProperties;
import com.huangjie.commoniam.dto.CreateUserRequest;
import com.huangjie.commoniam.dto.RegisterRequest;
import com.huangjie.commoniam.exception.BusinessException;
import com.huangjie.commoniam.vo.LoginResponse;
import com.huangjie.commoniam.vo.UserVO;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证业务编排服务。
 *
 * <p>这里不直接保存用户、密码或角色，所有用户身份与角色数据都来自 Keycloak。
 * IAM 只负责在 Keycloak 校验成功后签发自己的业务 token，并通过 HttpOnly Cookie 写回前端。</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_REGISTER_ROLE = "user";
    private static final String DEFAULT_REGISTER_ROLE_DESCRIPTION = "普通注册用户";

    private final KeycloakLoginService keycloakLoginService;
    private final KeycloakUserService keycloakUserService;
    private final KeycloakRoleService keycloakRoleService;
    private final TokenIssueService tokenIssueService;
    private final MockCaptchaService mockCaptchaService;
    private final AuthCookieProperties cookieProperties;

    /**
     * 普通用户自助注册。
     *
     * <p>注册接口只创建启用状态的普通用户，不允许前端指定角色。
     * 注册前会确保默认普通 Realm Role 存在，创建成功后自动分配该角色。</p>
     */
    public LoginResponse register(RegisterRequest request) {
        Map<String, Object> defaultRole = keycloakRoleService.ensureRoleRepresentation(
                DEFAULT_REGISTER_ROLE,
                DEFAULT_REGISTER_ROLE_DESCRIPTION
        );
        CreateUserRequest createUserRequest = new CreateUserRequest(
                request.username(),
                request.password(),
                request.email(),
                request.username(),
                request.username(),
                true,
                Map.of()
        );
        String userId = keycloakUserService.createUser(createUserRequest);
        assignDefaultRole(userId, defaultRole);
        List<String> roles = keycloakUserService.getUserRealmRoleNames(userId);
        return new LoginResponse(userId, request.username(), roles);
    }

    /**
     * 登录流程：
     * 1. 调用 Keycloak token 接口校验用户名密码；
     * 2. 查询 Keycloak 用户与 Realm Role；
     * 3. 签发业务 access_token / refresh_token；
     * 4. 将两个 token 写入 HttpOnly Cookie。
     */
    public LoginResponse login(String username, String password, HttpServletResponse response) {
        keycloakLoginService.verifyUsernamePassword(username, password);
        UserVO user = keycloakUserService.findSingleUserByUsername(username);
        List<String> roles = keycloakUserService.getUserRealmRoleNames(user.id());
        String accessToken = tokenIssueService.issueAccessToken(user.id(), user.username(), roles);
        String refreshToken = tokenIssueService.issueRefreshToken(user.id(), user.username());
        addAccessTokenCookie(response, accessToken);
        addRefreshTokenCookie(response, refreshToken);

        return new LoginResponse(user.id(), user.username(), roles);
    }

    /**
     * 生成模拟登录验证码。
     *
     * <p>验证码会输出到服务端日志，前端本地联调时从日志复制验证码再调用验证码登录接口。</p>
     */
    public boolean sendCaptcha(String username) {
        mockCaptchaService.generate(username);
        return true;
    }

    /**
     * 验证码登录流程。
     *
     * <p>验证码只证明调用方知道当前 mock code；用户仍然从 Keycloak 查询，
     * 并在登录前确认用户未被禁用。</p>
     */
    public LoginResponse captchaLogin(String username, String code, HttpServletResponse response) {
        mockCaptchaService.verify(username, code);
        UserVO user = keycloakUserService.findSingleUserByUsername(username);
        keycloakUserService.assertUserEnabled(user.id());
        List<String> roles = keycloakUserService.getUserRealmRoleNames(user.id());
        String accessToken = tokenIssueService.issueAccessToken(user.id(), user.username(), roles);
        String refreshToken = tokenIssueService.issueRefreshToken(user.id(), user.username());
        addAccessTokenCookie(response, accessToken);
        addRefreshTokenCookie(response, refreshToken);

        return new LoginResponse(user.id(), user.username(), roles);
    }

    /**
     * 刷新 access_token。
     *
     * <p>refresh_token 只由 IAM 自己校验，不需要也不应该交给 Gateway 校验。
     * 第一版不轮换 refresh_token，仅在校验通过后重发 access_token。</p>
     */
    public boolean refresh(String refreshToken, HttpServletResponse response) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh_token 不能为空");
        }
        Jwt jwt = tokenIssueService.verifyRefreshToken(refreshToken);
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("username");

        keycloakUserService.assertUserEnabled(userId);
        List<String> roles = keycloakUserService.getUserRealmRoleNames(userId);
        String accessToken = tokenIssueService.issueAccessToken(userId, username, roles);
        addAccessTokenCookie(response, accessToken);
        return true;
    }

    /**
     * 登出时清空浏览器中的 access_token 与 refresh_token Cookie。
     */
    public boolean logout(HttpServletResponse response) {
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
        return true;
    }

    /**
     * 给注册用户分配默认普通角色。
     */
    private void assignDefaultRole(String userId, Map<String, Object> defaultRole) {
        keycloakUserService.assignRealmRoles(userId, List.of(defaultRole));
    }

    /**
     * access_token Cookie 对整个站点生效，便于 Gateway 从 Cookie 中读取并验签。
     */
    private void addAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = baseCookie(cookieProperties.getAccessTokenName(), token)
                .path("/")
                .maxAge(tokenIssueService.accessTokenMaxAgeSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * refresh_token Cookie 只限制在 /iam/auth 路径下，减少被业务接口携带的范围。
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = baseCookie(cookieProperties.getRefreshTokenName(), token)
                .path("/iam/auth")
                .maxAge(tokenIssueService.refreshTokenMaxAgeSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Max-Age=0 表示让浏览器删除对应 Cookie。
     */
    private void clearAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie(cookieProperties.getAccessTokenName(), "")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 清理 refresh_token 时必须使用和写入时相同的 Path，否则浏览器不会删除原 Cookie。
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie(cookieProperties.getRefreshTokenName(), "")
                .path("/iam/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Cookie 的公共安全属性。
     * 本地开发 secure=false；生产 HTTPS 环境建议改为 true。
     */
    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite());
    }
}
