package tech.bytesmind.logistics.auth.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.tenancy.model.TenantAware;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entité User (utilisateur de la plateforme).
 * Source de vérité unique pour l'identité, l'actorType et l'affiliation agence.
 * La logique métier est dans UserDomainService.
 * <p>
 * Règles métier:
 * - AGENCY_EMPLOYEE : doit avoir un agencyId (et un seul)
 * - PLATFORM_ADMIN : agencyId doit être null
 * - CUSTOMER : agencyId doit être null
 * <p>
 * Note: L'authentification se fait via JWT OAuth2 externe (Keycloak).
 * Cette entité alimente les claims JWT (sub, actor_type, agency_id, roles).
 */
@Setter
@Getter
@Entity
@Table(name = "platform_user")
@SQLDelete(sql = "UPDATE platform_user SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class User implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(length = 50)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ActorType actorType;

    /**
     * Agency ID pour les AGENCY_EMPLOYEE uniquement.
     * Doit être null pour PLATFORM_ADMIN et CUSTOMER.
     * Un employé travaille pour UNE SEULE agence.
     */
    @Column(name = "agency_id")
    private UUID agencyId;

    /**
     * Identifiant externe dans le système OAuth (Keycloak).
     * Correspond au claim 'sub' du JWT.
     */
    @Column(name = "external_auth_id", unique = true)
    private String externalAuthId;

    /**
     * Nom d'utilisateur unique pour l'authentification locale.
     * Format: alphanumeric, underscores, hyphens, 3-50 caractères.
     */
    @Column(unique = true, length = 50)
    private String username;

    /**
     * Hash BCrypt du mot de passe pour l'authentification locale.
     * Optionnel si l'utilisateur utilise uniquement OAuth externe.
     */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * Titre/fonction (pour AGENCY_EMPLOYEE).
     */
    @Column(name = "job_title", length = 100)
    private String jobTitle;

    /**
     * Département (pour AGENCY_EMPLOYEE).
     */
    @Column(length = 100)
    private String department;

    // État
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    // Relations
    @ManyToMany
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // Métadonnées
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Lifecycle
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // TenantAware (pour AGENCY_EMPLOYEE uniquement)
    @Override
    public UUID getAgencyId() {
        return agencyId;
    }

    @Override
    public void setAgencyId(UUID agencyId) {
        this.agencyId = agencyId;
    }

    // Business helpers
    public boolean isPlatformAdmin() {
        return actorType == ActorType.PLATFORM_ADMIN;
    }

    public boolean isCustomer() {
        return actorType == ActorType.CUSTOMER;
    }

    public boolean isAgencyEmployee() {
        return actorType == ActorType.AGENCY_EMPLOYEE;
    }

    public boolean hasRole(String roleCode) {
        return roles.stream().anyMatch(r -> r.getCode().equals(roleCode));
    }

    public Set<String> getRoleCodes() {
        Set<String> roleCodes = new HashSet<>();
        if (roles != null) {
            roles.stream()
                    .filter(Role::isActive)
                    .forEach(r -> roleCodes.add(r.getCode()));
        }
        return roleCodes;
    }
}