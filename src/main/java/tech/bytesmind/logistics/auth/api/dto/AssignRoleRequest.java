package tech.bytesmind.logistics.auth.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignRoleRequest(
        @NotNull UUID roleId
) {
}
