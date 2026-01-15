package tech.bytesmind.logistics.auth.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité Role (rôle applicatif).
 * Définit les permissions fonctionnelles.
 * <p>
 * Exemples: AGENCY_ADMIN, SHIPMENT_MANAGER, PARCEL_MANAGER,
 * DELIVERY_DRIVER, SORTING_OPERATOR, etc.
 */
@Setter
@Getter
@Entity
@Table(name = "role")
public class

Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Scope du rôle: PLATFORM (transverse) ou AGENCY (scopé agence).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleScope scope = RoleScope.AGENCY;

    @Column(nullable = false)
    private boolean active = true;

    // Métadonnées
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    // Business helpers
    public boolean isPlatformScope() {
        return scope == RoleScope.PLATFORM;
    }

    public boolean isAgencyScope() {
        return scope == RoleScope.AGENCY;
    }

    public boolean isCustomerScope() {
        return scope == RoleScope.CUSTOMER;
    }
}