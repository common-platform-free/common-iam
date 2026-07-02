package com.huangjie.commoniam.config;

import com.huangjie.commoniam.service.KeycloakErrorMapper;
import feign.Request;
import feign.codec.ErrorDecoder;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak Feign 公共配置。
 *
 * <p>所有 Keycloak FeignClient 共用这里的超时配置和错误映射。
 * FeignClient 本身只描述外部接口，业务语义仍然放在 service 层。</p>
 */
@RequiredArgsConstructor
@Configuration
public class KeycloakFeignConfig {

    private final KeycloakProperties keycloakProperties;
    private final KeycloakErrorMapper keycloakErrorMapper;

    /**
     * 使用 application.yml 中的 Keycloak 超时配置。
     */
    @Bean
    public Request.Options keycloakFeignOptions() {
        return new Request.Options(
                keycloakProperties.getConnectTimeoutSeconds(), TimeUnit.SECONDS,
                keycloakProperties.getReadTimeoutSeconds(), TimeUnit.SECONDS,
                true
        );
    }

    /**
     * 将 Keycloak HTTP 错误统一映射成 BusinessException。
     */
    @Bean
    public ErrorDecoder keycloakErrorDecoder() {
        return (methodKey, response) ->
                keycloakErrorMapper.toBusinessException(methodKey, response.status(), response.reason());
    }
}
