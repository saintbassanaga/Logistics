package tech.bytesmind.logistics.agency.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.TenantId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import tech.bytesmind.logistics.shared.tenancy.model.TenantAware;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entité AgencyLocation (anémique - pas de logique métier).
 * Représente une localisation/siège d'une agence dans une ville.
 * La logique métier est dans LocationDomainService.
 */
@Setter
@Getter
@Entity
@Table(name = "agency_location")
@SQLDelete(sql = "UPDATE agency_location SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class AgencyLocation implements TenantAware {

    // Getters & Setters
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 50)
    private LocationType locationType = LocationType.BRANCH;

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

    // Coordonnées géographiques
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    // Contact
    private String email;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(name = "contact_person_name")
    private String contactPersonName;

    @Column(name = "contact_person_phone", length = 50)
    private String contactPersonPhone;

    // Opérationnel
    @Column(name = "opening_hours", columnDefinition = "TEXT")
    private String openingHours;

    // Capacités
    @Column(name = "max_daily_parcels")
    private Integer maxDailyParcels;

    @Column(name = "storage_capacity_m3", precision = 10, scale = 2)
    private BigDecimal storageCapacityM3;

    // État
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "temporarily_closed", nullable = false)
    private boolean temporarilyClosed = false;

    @Column(name = "closure_reason", columnDefinition = "TEXT")
    private String closureReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "agency_id", nullable = false, unique = true)
    private Agency agency;

    @Override
    public UUID getAgencyId() {
        return agency.getAgencyId();
    }

    @Override
    public void setAgencyId(UUID agencyId) {
    }
}
