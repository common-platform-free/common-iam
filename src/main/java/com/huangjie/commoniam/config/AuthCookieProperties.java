package com.huangjie.commoniam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证 Cookie 配置。
 *
 * <p>用于控制 access_token / refresh_token 的 Cookie 名称、SameSite 和 Secure 属性。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieProperties {

    private String accessTokenName;
    private String refreshTokenName;
    private boolean secure;
    private String sameSite;
}
