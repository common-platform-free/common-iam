package com.huangjie.commoniam.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 移除用户角色的请求体。
 */
public record RemoveRoleRequest(
        @NotEmpty(message = "角色名称列表不能为空") List<String> roleNames
) {
}
