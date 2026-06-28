package com.huangjie.commoniam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 基础配置。
 *
 * <p>引入 spring-boot-starter-security 后，如果不显式配置，Spring Security 会默认拦截接口。
 * 本项目第一版由 Gateway 统一校验 access_token，IAM 不作为 Resource Server，
 * 因此这里放行 IAM 接口，避免和 Gateway 鉴权边界冲突。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 放行 IAM 服务全部请求。
     * 生产环境必须通过网络隔离保证 IAM 只能被 Gateway 访问，不能公网直连。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 第一版 IAM 不作为 Resource Server 校验业务 access_token。
        // 认证鉴权由 Gateway 负责，IAM 服务本地全部放行。
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .build();
    }
}
