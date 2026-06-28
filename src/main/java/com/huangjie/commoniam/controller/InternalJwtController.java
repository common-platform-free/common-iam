package com.huangjie.commoniam.controller;

import com.huangjie.commoniam.common.ApiResult;
import com.huangjie.commoniam.config.JwtProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部 JWT 配置接口。
 *
 * <p>access_token 使用 RS256 后，Gateway 只需要获取公钥验签。
 * IAM 私钥不通过任何接口暴露。</p>
 */
@RestController
@RequestMapping("/iam/internal/jwt")
@RequiredArgsConstructor
public class InternalJwtController {

    private final JwtProperties jwtProperties;

    /**
     * 返回 Gateway 验 access_token 所需的 PEM 公钥。
     * refresh-secret 不对外暴露，只在 IAM 内部使用。
     */
    @GetMapping("/public-key")
    public ApiResult<Map<String, String>> getJwtPublicKey() {
        return ApiResult.success(Map.of(
                "alg", "RS256",
                "publicKey", jwtProperties.getAccessPublicKey()
        ));
    }
}
