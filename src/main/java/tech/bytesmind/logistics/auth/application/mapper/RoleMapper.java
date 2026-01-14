package tech.bytesmind.logistics.auth.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import tech.bytesmind.logistics.auth.api.dto.CreateRoleRequest;
import tech.bytesmind.logistics.auth.api.dto.RoleResponse;
import tech.bytesmind.logistics.auth.api.dto.UpdateRoleRequest;
import tech.bytesmind.logistics.auth.domain.model.Role;

import java.util.List;

/**
 * MapStruct mapper pour les conversions Role ↔ DTOs.
 */
@Mapper(componentModel = "spring")
public interface RoleMapper {

    /**
     * Convertit CreateRoleRequest → Role entity.
     * Les champs id, active et timestamps sont ignorés (gérés par le service ou JPA).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Role toEntity(CreateRoleRequest request);

    /**
     * Convertit Role entity → RoleResponse DTO.
     */
    RoleResponse toResponse(Role role);

    /**
     * Convertit une liste de rôles en liste de réponses.
     */
    List<RoleResponse> toResponseList(List<Role> roles);

    /**
     * Met à jour un Role existant avec les données d'UpdateRoleRequest.
     * Ne touche PAS aux champs: id, code, scope, active, timestamps.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "scope", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateRoleRequest request, @MappingTarget Role role);
}
