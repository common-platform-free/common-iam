package com.huangjie.commoniam.controller;

import com.huangjie.commoniam.common.ApiResult;
import com.huangjie.commoniam.config.AuthCookieProperties;
import com.huangjie.commoniam.dto.CaptchaLoginRequest;
import com.huangjie.commoniam.dto.CaptchaSendRequest;
import com.huangjie.commoniam.dto.LoginRequest;
import com.huangjie.commoniam.dto.RegisterRequest;
import com.huangjie.commoniam.service.AuthService;
import com.huangjie.commoniam.vo.CurrentUserVO;
import com.huangjie.commoniam.vo.LoginResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 *
 * <p>提供自定义登录、刷新、登出和当前用户信息接口。
 * access_token 的校验由 Gateway 完成，IAM 第一版只负责签发和刷新 token。</p>
 */
@RestController
@RequestMapping("/iam/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieProperties cookieProperties;

    /**
     * 注册接口。
     * 普通用户自助注册，注册成功后返回用户基础信息，不自动登录。
     */
    @PostMapping("/register")
    public ApiResult<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.success(authService.register(request));
    }

    /**
     * 登录接口。
     * 登录成功后通过 Set-Cookie 写入 access_token 与 refresh_token。
     */
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return ApiResult.success(authService.login(request.username(), request.password(), response));
    }

    /**
     * 生成模拟验证码。
     * 验证码不会返回给前端，只会输出到服务端日志。
     */
    @PostMapping("/captcha/send")
    public ApiResult<Boolean> sendCaptcha(@Valid @RequestBody CaptchaSendRequest request) {
        return ApiResult.success(authService.sendCaptcha(request.username()));
    }

    /**
     * 验证码登录接口。
     * 校验成功后通过 Set-Cookie 写入 access_token 与 refresh_token。
     */
    @PostMapping("/captcha/login")
    public ApiResult<LoginResponse> captchaLogin(
            @Valid @RequestBody CaptchaLoginRequest request,
            HttpServletResponse response
    ) {
        return ApiResult.success(authService.captchaLogin(request.username(), request.code(), response));
    }

    /**
     * 刷新 access_token。
     * refresh_token 从 HttpOnly Cookie 中读取，前端无需也不应该从 JS 中拿到它。
     */
    @PostMapping("/refresh")
    public ApiResult<Boolean> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return ApiResult.success(authService.refresh(resolveRefreshToken(refreshToken, request), response));
    }

    /**
     * 登出接口。
     * 通过清理 Cookie 完成本地登出。
     */
    @PostMapping("/logout")
    public ApiResult<Boolean> logout(HttpServletResponse response) {
        return ApiResult.success(authService.logout(response));
    }

    /**
     * 当前用户信息。
     * IAM 第一版信任 Gateway 校验后透传的用户 Header。
     */
    @GetMapping("/me")
    public ApiResult<CurrentUserVO> me(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Roles", required = false) String roles
    ) {
        // 第一版信任 Gateway 透传 Header。生产环境必须禁止公网直连 IAM，否则可绕过 Gateway 伪造身份。
        return ApiResult.success(new CurrentUserVO(userId, username, parseRoles(roles)));
    }

    /**
     * 兼容配置化 Cookie 名称。
     * @CookieValue 的 name 必须是常量，所以这里再从 request 中按配置名称兜底读取。
     */
    private String resolveRefreshToken(String defaultCookieValue, HttpServletRequest request) {
        if (StringUtils.hasText(defaultCookieValue)) {
            return defaultCookieValue;
        }
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieProperties.getRefreshTokenName().equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    /**
     * Gateway 透传的 X-Roles 使用逗号分隔，这里转换成列表返回给前端。
     */
    private List<String> parseRoles(String roles) {
        if (!StringUtils.hasText(roles)) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
