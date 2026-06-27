package com.huangjie.commoniam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置。
 *
 * <p>access-secret 提供给 Gateway 验签 access_token；
 * refresh-secret 只在 IAM 内部用于 refresh_token。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String accessSecret;
    private String refreshSecret;
    private long accessTokenExpireMinutes;
    private long refreshTokenExpireDays;
}
