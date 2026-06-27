package com.huangjie.commoniam.vo;

import java.util.List;

/**
 * 当前登录用户信息。
 *
 * <p>来源于 Gateway 校验后透传的 X-User-* Header。</p>
 */
public record CurrentUserVO(
        String userId,
        String username,
        List<String> roles
) {
}
