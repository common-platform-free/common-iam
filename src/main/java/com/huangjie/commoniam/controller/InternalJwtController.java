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
 * <p>当前 Gateway 仍使用 HS256 自定义验签，因此本地/内网环境提供 access-secret 查询接口。
 * 生产环境不建议暴露任何对称密钥，后续应迁移到 RS256 公私钥模式。</p>
 */
@RestController
@RequestMapping("/iam/internal/jwt")
@RequiredArgsConstructor
public class InternalJwtController {

    private final JwtProperties jwtProperties;

    /**
     * 返回 Gateway 验 access_token 所需的算法和 access-secret。
     * refresh-secret 不对外暴露，只在 IAM 内部使用。
     */
    @GetMapping("/secret")
    public ApiResult<Map<String, String>> getJwtSecret() {
        // 仅用于本地开发或可信内网。这里只返回 access-secret 给 Gateway 验签。
        // refresh-secret 只留在 IAM 内部，生产环境不建议暴露任何 HS256 secret，后续应迁移到 RS256 公私钥模式。
        return ApiResult.success(Map.of(
                "alg", "HS256",
                "secret", jwtProperties.getAccessSecret()
        ));
    }
}
