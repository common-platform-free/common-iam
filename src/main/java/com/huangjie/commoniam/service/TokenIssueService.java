package com.huangjie.commoniam.service;

import com.huangjie.commoniam.common.ErrorCode;
import com.huangjie.commoniam.config.JwtConfig;
import com.huangjie.commoniam.config.JwtProperties;
import com.huangjie.commoniam.exception.BusinessException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

/**
 * 业务 JWT 签发与 refresh_token 校验服务。
 *
 * <p>access_token 使用 RS256 私钥签发，公钥给 Gateway 验签；
 * refresh_token 使用 IAM 内部 HS256 secret 签发和校验。</p>
 */
@Service
public class TokenIssueService {

    private static final String CLAIM_TYP = "typ";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";

    private final JwtEncoder accessTokenJwtEncoder;
    private final JwtEncoder refreshTokenJwtEncoder;
    private final JwtDecoder refreshTokenJwtDecoder;
    private final JwtProperties jwtProperties;

    public TokenIssueService(
            @Qualifier("accessTokenJwtEncoder") JwtEncoder accessTokenJwtEncoder,
            @Qualifier("refreshTokenJwtEncoder") JwtEncoder refreshTokenJwtEncoder,
            @Qualifier("refreshTokenJwtDecoder") JwtDecoder refreshTokenJwtDecoder,
            JwtProperties jwtProperties
    ) {
        this.accessTokenJwtEncoder = accessTokenJwtEncoder;
        this.refreshTokenJwtEncoder = refreshTokenJwtEncoder;
        this.refreshTokenJwtDecoder = refreshTokenJwtDecoder;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 签发业务 access_token。
     * payload 中包含 typ=access、Keycloak userId、username 和 Realm Roles。
     */
    public String issueAccessToken(String userId, String username, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.getAccessTokenExpireMinutes(), ChronoUnit.MINUTES))
                .claim(CLAIM_TYP, "access")
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, roles)
                .build();
        return encodeAccessToken(claims);
    }

    /**
     * 签发 refresh_token。
     * refresh_token 不包含角色，刷新时会重新从 Keycloak 查询最新角色。
     */
    public String issueRefreshToken(String userId, String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.getRefreshTokenExpireDays(), ChronoUnit.DAYS))
                .claim(CLAIM_TYP, "refresh")
                .claim(CLAIM_USERNAME, username)
                .build();
        return encodeRefreshToken(claims);
    }

    /**
     * 校验 refresh_token 签名、过期时间和 typ。
     * 如果前端误传 access_token 到刷新接口，这里会因为 typ 不匹配而拒绝。
     */
    public Jwt verifyRefreshToken(String refreshToken) {
        try {
            Jwt jwt = refreshTokenJwtDecoder.decode(refreshToken);
            if (!"refresh".equals(jwt.getClaimAsString(CLAIM_TYP))) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh_token 类型错误");
            }
            return jwt;
        } catch (JwtException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh_token 无效或已过期");
        }
    }

    /**
     * access_token Cookie 的 Max-Age，和 token exp 保持一致。
     */
    public long accessTokenMaxAgeSeconds() {
        return jwtProperties.getAccessTokenExpireMinutes() * 60;
    }

    /**
     * refresh_token Cookie 的 Max-Age，和 token exp 保持一致。
     */
    public long refreshTokenMaxAgeSeconds() {
        return jwtProperties.getRefreshTokenExpireDays() * 24 * 60 * 60;
    }

    /**
     * 使用 Spring Security OAuth2 JOSE 组件完成 RS256 签名，不手写 JWT 拼接逻辑。
     */
    private String encodeAccessToken(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(JwtConfig.ACCESS_TOKEN_KEY_ID)
                .build();
        return accessTokenJwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * 使用 Spring Security OAuth2 JOSE 组件完成 refresh_token 的 HS256 签名。
     */
    private String encodeRefreshToken(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return refreshTokenJwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
