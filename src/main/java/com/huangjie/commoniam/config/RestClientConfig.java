package com.huangjie.commoniam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * HTTP 客户端配置。
 *
 * <p>Keycloak 登录接口和 Admin API 都通过这个 RestClient 访问，
 * baseUrl、连接超时、读取超时来自 application.yml。</p>
 */
@Configuration
public class RestClientConfig {

    /**
     * Keycloak 专用 RestClient。
     */
    @Bean
    public RestClient keycloakRestClient(RestClient.Builder builder, KeycloakProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutSeconds() * 1000);
        requestFactory.setReadTimeout(properties.getReadTimeoutSeconds() * 1000);
        return builder
                .baseUrl(properties.getServerUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
