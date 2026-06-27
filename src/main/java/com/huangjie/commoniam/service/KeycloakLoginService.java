package com.huangjie.commoniam.service;

import com.huangjie.commoniam.config.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Keycloak 用户名密码校验服务。
 *
 * <p>IAM 不读取、不保存用户密码。登录时只调用 Keycloak token 接口，
 * 如果 Keycloak 返回成功，就表示用户名密码正确。</p>
 */
@Service
@RequiredArgsConstructor
public class KeycloakLoginService {

    private final RestClient keycloakRestClient;
    private final KeycloakProperties keycloakProperties;
    private final KeycloakErrorMapper keycloakErrorMapper;

    /**
     * 使用 password grant 校验用户名密码。
     * Keycloak 返回的 token 不返回给前端，只作为密码校验成功的信号。
     */
    public void verifyUsernamePassword(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakProperties.getLoginClientId());
        form.add("client_secret", keycloakProperties.getLoginClientSecret());
        form.add("username", username);
        form.add("password", password);

        try {
            keycloakRestClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", keycloakProperties.getRealm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw keycloakErrorMapper.toBusinessException(ex);
        }
    }
}
