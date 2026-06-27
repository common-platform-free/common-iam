package com.huangjie.commoniam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Keycloak 连接配置。
 *
 * <p>admin client 用于调用 Admin API；login client 用于登录时校验用户名密码。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String serverUrl;
    private String realm;
    private String adminClientId;
    private String adminClientSecret;
    private String loginClientId;
    private String loginClientSecret;
    private int connectTimeoutSeconds;
    private int readTimeoutSeconds;
}
