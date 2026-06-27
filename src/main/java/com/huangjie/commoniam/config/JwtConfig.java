package com.huangjie.commoniam.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * JWT 相关 Bean 配置。
 *
 * <p>本项目第一版使用 HS256，并且 access_token 与 refresh_token 使用不同密钥。
 * Gateway 只拿 access-secret 验 access_token，refresh-secret 只留在 IAM 内部。</p>
 */
@Configuration
public class JwtConfig {

    /**
     * access_token 使用的 HS256 密钥。
     * Gateway 只需要拿到这个密钥来验业务 access_token。
     */
    @Bean("accessTokenSecretKey")
    public SecretKey accessTokenSecretKey(JwtProperties jwtProperties) {
        return hmacSha256Key(jwtProperties.getAccessSecret());
    }

    /**
     * refresh_token 使用独立 HS256 密钥，只保留在 IAM 服务内部。
     */
    @Bean("refreshTokenSecretKey")
    public SecretKey refreshTokenSecretKey(JwtProperties jwtProperties) {
        return hmacSha256Key(jwtProperties.getRefreshSecret());
    }

    /**
     * 签发业务 access_token。
     * IAM 第一版只签发 access_token，不在本服务内校验它，校验由 Gateway 完成。
     */
    @Bean("accessTokenJwtEncoder")
    public JwtEncoder accessTokenJwtEncoder(@Qualifier("accessTokenSecretKey") SecretKey accessTokenSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(accessTokenSecretKey));
    }

    /**
     * 签发 refresh_token，供前端在 access_token 过期后调用刷新接口使用。
     */
    @Bean("refreshTokenJwtEncoder")
    public JwtEncoder refreshTokenJwtEncoder(@Qualifier("refreshTokenSecretKey") SecretKey refreshTokenSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(refreshTokenSecretKey));
    }

    /**
     * 校验 refresh_token。
     * 这里没有配置 access_token decoder，因为 IAM 第一版不作为 Resource Server。
     */
    @Bean("refreshTokenJwtDecoder")
    public JwtDecoder refreshTokenJwtDecoder(@Qualifier("refreshTokenSecretKey") SecretKey refreshTokenSecretKey) {
        return NimbusJwtDecoder.withSecretKey(refreshTokenSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * 将配置字符串转换成 HMAC-SHA256 SecretKey，供 NimbusJwtEncoder/NimbusJwtDecoder 使用。
     */
    private SecretKey hmacSha256Key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
