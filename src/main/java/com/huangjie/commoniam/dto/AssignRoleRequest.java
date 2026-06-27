package com.huangjie.commoniam.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 给用户分配角色的请求体。
 */
public record AssignRoleRequest(
        @NotEmpty(message = "角色名称列表不能为空") List<String> roleNames
) {
}
