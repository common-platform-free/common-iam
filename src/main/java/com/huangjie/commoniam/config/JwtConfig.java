package com.huangjie.commoniam.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
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
 * <p>access_token 使用 RS256：IAM 用私钥签发，Gateway 或 Resource Server 用公钥验签。
 * refresh_token 只在 IAM 内部使用，继续使用独立 HS256 secret 签发和校验。</p>
 */
@Configuration
public class JwtConfig {

    public static final String ACCESS_TOKEN_KEY_ID = "iam-access-key";

    /**
     * access_token 私钥，只保留在 IAM 内部，用于签发 access_token。
     */
    @Bean("accessTokenPrivateKey")
    public RSAPrivateKey accessTokenPrivateKey(JwtProperties jwtProperties) {
        return parseRsaPrivateKey(jwtProperties.getAccessPrivateKey());
    }

    /**
     * access_token 公钥，可以提供给 Gateway 验签。
     */
    @Bean("accessTokenPublicKey")
    public RSAPublicKey accessTokenPublicKey(JwtProperties jwtProperties) {
        return parseRsaPublicKey(jwtProperties.getAccessPublicKey());
    }

    /**
     * refresh_token 使用的 HS256 密钥，只保留在 IAM 服务内部。
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
    public JwtEncoder accessTokenJwtEncoder(
            @Qualifier("accessTokenPublicKey") RSAPublicKey publicKey,
            @Qualifier("accessTokenPrivateKey") RSAPrivateKey privateKey
    ) {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(ACCESS_TOKEN_KEY_ID)
                .build();
        JWKSet accessTokenJwkSet = new JWKSet(rsaKey);
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(accessTokenJwkSet));
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
     * 将配置字符串转换成 HMAC-SHA256 SecretKey，供 refresh_token 使用。
     */
    private SecretKey hmacSha256Key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * 解析 PEM 格式 PKCS#8 RSA 私钥。
     */
    private RSAPrivateKey parseRsaPrivateKey(String pem) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(stripPem(pem));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception ex) {
            throw new IllegalStateException("access-private-key 解析失败", ex);
        }
    }

    /**
     * 解析 PEM 格式 X.509 RSA 公钥。
     */
    private RSAPublicKey parseRsaPublicKey(String pem) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(stripPem(pem));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception ex) {
            throw new IllegalStateException("access-public-key 解析失败", ex);
        }
    }

    /**
     * 去掉 PEM 头尾和空白字符，只保留 Base64 内容。
     */
    private String stripPem(String pem) {
        return pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }
}
