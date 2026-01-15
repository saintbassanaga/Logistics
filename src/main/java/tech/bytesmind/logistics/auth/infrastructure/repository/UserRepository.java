package tech.bytesmind.logistics.auth.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.shared.security.model.ActorType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'entité User.
 * Source de vérité pour l'authentification et l'autorisation.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByExternalAuthId(String externalAuthId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByActorType(ActorType actorType);

    List<User> findByAgencyId(UUID agencyId);

    List<User> findByAgencyIdAndActiveTrue(UUID agencyId);

    @Query("SELECT u FROM User u WHERE u.actorType = 'AGENCY_EMPLOYEE' AND u.agencyId = :agencyId")
    List<User> findAgencyEmployees(@Param("agencyId") UUID agencyId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") UUID id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.externalAuthId = :externalAuthId")
    Optional<User> findByExternalAuthIdWithRoles(@Param("externalAuthId") String externalAuthId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :identifier OR u.username = :identifier")
    Optional<User> findByEmailOrUsernameWithRoles(@Param("identifier") String identifier);

    @Query("SELECT u FROM User u WHERE u.active = true AND u.emailVerified = true")
    List<User> findAllActiveVerified();

    @Query("SELECT COUNT(u) FROM User u WHERE u.agencyId = :agencyId AND u.actorType = 'AGENCY_EMPLOYEE' AND u.active = true")
    long countActiveEmployeesByAgency(@Param("agencyId") UUID agencyId);
}