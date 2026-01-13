package tech.bytesmind.logistics.agency.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import tech.bytesmind.logistics.shared.tenancy.model.TenantAware;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
    name = "agency_location",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_agency_location_code", columnNames = {"agency_id", "code"})
    },
    indexes = {
        @Index(name = "idx_agency_location_agency_id", columnList = "agency_id"),
        @Index(name = "idx_agency_location_code", columnList = "code"),
        @Index(name = "idx_agency_location_city", columnList = "city"),
        @Index(name = "idx_agency_location_country", columnList = "country"),
        @Index(name = "idx_agency_location_active", columnList = "active"),
        @Index(name = "idx_agency_location_type", columnList = "location_type")
    }
)
public class AgencyLocation implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", insertable = false, updatable = false)
    private com.transport.platform.agency.infrastructure.Agency agency;
    
    @Column(name = "code", nullable = false, length = 50)
    private String code;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 50)
    private LocationType locationType = LocationType.BRANCH;


    @Column(name = "address_line1", nullable = false)
    private String addressLine1;
    
    @Column(name = "address_line2")
    private String addressLine2;
    
    @Column(name = "city", nullable = false, length = 100)
    private String city;
    
    @Column(name = "state_region", length = 100)
    private String stateRegion;
    
    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;
    
    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "email")
    private String email;
    
    @Column(name = "phone", nullable = false, length = 50)
    private String phone;
    
    @Column(name = "contact_person_name")
    private String contactPersonName;
    
    @Column(name = "contact_person_phone", length = 50)
    private String contactPersonPhone;
    
    @Column(name = "opening_hours", columnDefinition = "TEXT")
    private String openingHours; // JSON format
    
    @Column(name = "is_pickup_point", nullable = false)
    private boolean isPickupPoint = true;
    
    @Column(name = "is_delivery_point", nullable = false)
    private boolean isDeliveryPoint = true;
    
    @Column(name = "is_warehouse", nullable = false)
    private boolean isWarehouse = false;

    @Column(name = "max_daily_parcels")
    private Integer maxDailyParcels;
    
    @Column(name = "storage_capacity_m3", precision = 10, scale = 2)
    private BigDecimal storageCapacityM3;

    @Column(name = "active", nullable = false)
    private boolean active = true;
    
    @Column(name = "temporarily_closed", nullable = false)
    private boolean temporarilyClosed = false;
    
    @Column(name = "closure_reason", columnDefinition = "TEXT")
    private String closureReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "deleted_at")
    private Instant deletedAt;


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
    
    // =======================================================================
    // BUSINESS METHODS
    // =======================================================================
    
    public boolean isOperational() {
        return active && !temporarilyClosed;
    }
    
    public boolean canAcceptPickup() {
        return isOperational() && isPickupPoint;
    }
    
    public boolean canAcceptDelivery() {
        return isOperational() && isDeliveryPoint;
    }
    
    public boolean hasReachedDailyCapacity(int currentDailyParcels) {
        return maxDailyParcels != null && currentDailyParcels >= maxDailyParcels;
    }
    
    public void temporaryClose(String reason) {
        this.temporarilyClosed = true;
        this.closureReason = reason;
    }
    
    public void reopen() {
        this.temporarilyClosed = false;
        this.closureReason = null;
    }
    
    public void deactivate() {
        this.active = false;
    }
    
    public void activate() {
        this.active = true;
        this.temporarilyClosed = false;
        this.closureReason = null;
    }
    
    /**
     * Calculate la distance approximative en km verse une outre localisation.
     * Utilise la formula de Haversine.
     */
    public Double calculateDistanceKm(AgencyLocation other) {
        if (this.latitude == null || this.longitude == null 
            || other.latitude == null || other.longitude == null) {
            return null;
        }
        
        double lat1 = Math.toRadians(this.latitude.doubleValue());
        double lon1 = Math.toRadians(this.longitude.doubleValue());
        double lat2 = Math.toRadians(other.latitude.doubleValue());
        double lon2 = Math.toRadians(other.longitude.doubleValue());
        
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return 6371.0 * c; // Rayon de la Terre en km
    }

    @Override
    public UUID getAgencyId() {
        return agencyId;
    }
    
    @Override
    public void setAgencyId(UUID agencyId) {
        this.agencyId = agencyId;
    }

}