package tech.bytesmind.logistics.auth.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import tech.bytesmind.logistics.auth.api.dto.CreateUserRequest;
import tech.bytesmind.logistics.auth.api.dto.UpdateUserRequest;
import tech.bytesmind.logistics.auth.api.dto.UserResponse;
import tech.bytesmind.logistics.auth.domain.model.User;

import java.util.List;

/**
 * MapStruct mapper pour les conversions User ↔ DTOs.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Convertit CreateUserRequest → User entity.
     * Les champs id, timestamps, externalAuthId, active, emailVerified et roles
     * sont ignorés (gérés par le service).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalAuthId", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "username", source = "username")
    User toEntity(CreateUserRequest request);

    /**
     * Convertit User entity → UserResponse DTO.
     * Le champ roles est mappé via la méthode helper getRoleCodes().
     */
    @Mapping(target = "roles", expression = "java(user.getRoleCodes())")
    UserResponse toResponse(User user);

    /**
     * Convertit une liste d'utilisateurs en liste de réponses.
     */
    List<UserResponse> toResponseList(List<User> users);

    /**
     * Met à jour un User existant avec les données d'UpdateUserRequest.
     * Ne touche PAS aux champs: id, email, actorType, agencyId, externalAuthId,
     * active, emailVerified, roles, timestamps.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "actorType", ignore = true)
    @Mapping(target = "agencyId", ignore = true)
    @Mapping(target = "externalAuthId", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);
}
