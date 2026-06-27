package com.huangjie.commoniam.vo;

import java.util.List;

/**
 * 登录成功后返回给前端的用户基础信息。
 *
 * <p>token 不放在响应体中，而是写入 HttpOnly Cookie。</p>
 */
public record LoginResponse(
        String userId,
        String username,
        List<String> roles
) {
}
