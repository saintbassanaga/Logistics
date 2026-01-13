package com.transport.platform.agency.infrastructure;

import com.transport.platform.shared.tenancy.model.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import tech.bytesmind.logistics.agency.domain.AgencyLocation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité JPA représentant une agence de transport (tenant).
 *
 * Conforme à ADR-006 : Multi-tenance logique.
 * Implémente TenantAware bien que l'agence SOIT elle-même le tenant.
 */
@Setter
@Getter
@Entity
@Table(
        name = "agency",
        indexes = {
                @Index(name = "idx_agency_code", columnList = "code"),
                @Index(name = "idx_agency_active", columnList = "active"),
                @Index(name = "idx_agency_email", columnList = "email"),
                @Index(name = "idx_agency_subscription_tier", columnList = "subscription_tier"),
                @Index(name = "idx_agency_country", columnList = "country")
        }
)
@SQLDelete(sql = "UPDATE agency SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Agency implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "legal_name")
    private String legalName;


    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "website")
    private String website;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_region", length = 100)
    private String stateRegion;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 2)
    private String country; // ISO 3166-1 alpha-2


    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency = "USD"; // ISO 4217

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "UTC";

    @Column(name = "locale", nullable = false, length = 10)
    private String locale = "en_US";


    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "vat_number", length = 50)
    private String vatNumber;

    @Column(name = "transport_license_number", length = 100)
    private String transportLicenseNumber;

    @Column(name = "max_shipments_per_month")
    private Integer maxShipmentsPerMonth;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false, length = 50)
    private SubscriptionTier subscriptionTier = SubscriptionTier.BASIC;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "suspended", nullable = false)
    private boolean suspended = false;

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    private String suspensionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;


    @OneToMany(
            mappedBy = "agency",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<AgencyLocation> locations = new ArrayList<>();


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

    @Override
    public UUID getAgencyId() {
        return this.id;
    }

    @Override
    public void setAgencyId(UUID agencyId) {
    }

    // =======================================================================
    // BUSINESS METHODS
    // =======================================================================

    public boolean canCreateShipment() {
        return active && !suspended;
    }

    public boolean hasReachedUserLimit(int currentUserCount) {
        return maxUsers != null && currentUserCount >= maxUsers;
    }

    public boolean hasReachedShipmentLimit(int currentMonthShipments) {
        return maxShipmentsPerMonth != null && currentMonthShipments >= maxShipmentsPerMonth;
    }

    public void suspend(String reason) {
        this.suspended = true;
        this.suspensionReason = reason;
    }

    public void unsuspend() {
        this.suspended = false;
        this.suspensionReason = null;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void addLocation(AgencyLocation location) {
        locations.add(location);
        location.setAgency(this);
        location.setAgencyId(this.id);
    }

    public void removeLocation(AgencyLocation location) {
        locations.remove(location);
        location.setAgency(null);
    }


    public boolean hasHeadquarters() {
        return locations.stream()
                .anyMatch(loc -> loc.getLocationType() == com.transport.platform.agency.domain.LocationType.HEADQUARTERS
                        && loc.isActive());
    }


    public long countActiveLocations() {
        return locations.stream()
                .filter(AgencyLocation::isActive)
                .count();
    }

    public List<AgencyLocation> getOperationalLocations() {
        return locations.stream()
                .filter(AgencyLocation::isOperational)
                .toList();
    }

}