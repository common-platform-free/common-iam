package com.huangjie.commoniam.service;

import com.huangjie.commoniam.config.KeycloakProperties;
import com.huangjie.commoniam.exception.BusinessException;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Keycloak Admin API 访问令牌服务。
 *
 * <p>用户管理、角色管理等 Admin API 都需要 bearer token。
 * 这里使用 admin client 的 client_credentials 模式获取 token，并在内存中做短期缓存。</p>
 */
@Service
@RequiredArgsConstructor
public class KeycloakAdminTokenService {

    private final RestClient keycloakRestClient;
    private final KeycloakProperties keycloakProperties;
    private final KeycloakErrorMapper keycloakErrorMapper;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    /**
     * 获取可用于调用 Keycloak Admin API 的 access token。
     * token 过期前 60 秒会提前刷新，避免临界点请求失败。
     */
    public String getAdminToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) {
            return cachedToken;
        }
        synchronized (this) {
            if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) {
                return cachedToken;
            }
            Map<String, Object> response = requestAdminToken();
            cachedToken = (String) response.get("access_token");
            Number expiresIn = (Number) response.getOrDefault("expires_in", 300);
            expiresAt = Instant.now().plusSeconds(expiresIn.longValue());
            return cachedToken;
        }
    }

    /**
     * 通过 client_credentials 向 Keycloak 请求 admin token。
     */
    private Map<String, Object> requestAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", keycloakProperties.getAdminClientId());
        form.add("client_secret", keycloakProperties.getAdminClientSecret());

        try {
            return keycloakRestClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", keycloakProperties.getRealm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }
}
