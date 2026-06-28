package com.huangjie.commoniam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置。
 *
 * <p>access-private-key 用于 IAM 签发 access_token，access-public-key 提供给 Gateway 验签；
 * refresh-secret 只在 IAM 内部用于 refresh_token。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String accessPrivateKey;
    private String accessPublicKey;
    private String refreshSecret;
    private long accessTokenExpireMinutes;
    private long refreshTokenExpireDays;
}
