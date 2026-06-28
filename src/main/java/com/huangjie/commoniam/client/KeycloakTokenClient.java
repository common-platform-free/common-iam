package com.huangjie.commoniam.client;

import com.huangjie.commoniam.config.KeycloakFeignConfig;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Keycloak OIDC token 接口。
 *
 * <p>同时用于 login client 的 password grant 校验，以及 admin client 的 client_credentials 获取管理令牌。</p>
 */
@FeignClient(name = "keycloak-token-client", url = "${keycloak.server-url}", configuration = KeycloakFeignConfig.class)
public interface KeycloakTokenClient {

    /**
     * 调用 Keycloak token 端点。
     *
     * <p>路径：POST /realms/{realm}/protocol/openid-connect/token</p>
     *
     * <p>当前有两种用途：
     * 1. grant_type=password：校验用户输入的用户名密码；
     * 2. grant_type=client_credentials：获取调用 Admin API 的 admin token。</p>
     */
    @PostMapping(value = "/realms/{realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Map<String, Object> token(@PathVariable("realm") String realm, @RequestBody MultiValueMap<String, String> form);
}
