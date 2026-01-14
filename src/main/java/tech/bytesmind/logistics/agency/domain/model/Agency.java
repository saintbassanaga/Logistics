package tech.bytesmind.logistics.agency.domain.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import tech.bytesmind.logistics.shared.tenancy.model.TenantAware;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Entité Agency (anémique - pas de logique métier).
 * La logique métier est dans AgencyDomainService.
 */
@Setter
@Getter
@Entity
@Table(name = "agency")
@SQLDelete(sql = "UPDATE agency SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Agency implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "legal_name")
    private String legalName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 50)
    private String phone;

    private String website;

    // Adresse
    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "state_region", length = 100)
    private String stateRegion;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 2)
    private String country;

    // Configuration
    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency = "USD";

    @Column(nullable = false, length = 50)
    private String timezone = "UTC";

    @Column(nullable = false, length = 10)
    private String locale = "en_US";

    // Légal
    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "vat_number", length = 50)
    private String vatNumber;

    @Column(name = "transport_license_number", length = 100)
    private String transportLicenseNumber;

    // Quotas
    @Column(name = "max_shipments_per_month")
    private Integer maxShipmentsPerMonth;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false, length = 20)
    private SubscriptionTier subscriptionTier;

    // État
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean suspended = false;

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    private String suspensionReason;

    // Métadonnées
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "agency", orphanRemoval = true)
    private Collection<AgencyLocation> agencyLocations = new ArrayList<>();


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

    // TenantAware
    @Override
    public UUID getAgencyId() {
        return id;
    }

    @Override
    public void setAgencyId(UUID agencyId) {
        this.id = agencyId;
    }

}
