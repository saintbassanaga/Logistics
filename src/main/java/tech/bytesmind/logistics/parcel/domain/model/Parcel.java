package tech.bytesmind.logistics.parcel.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import tech.bytesmind.logistics.shared.tenancy.model.TenantAware;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entité Parcel (colis individuel).
 * Unité opérationnelle trackée individuellement.
 * Possède un lifecycle indépendant du Shipment.
 * La logique métier est dans ParcelDomainService.
 */
@Setter
@Getter
@Entity
@Table(name = "parcel")
@SQLDelete(sql = "UPDATE parcel SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Parcel implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Column(name = "tracking_number", unique = true, nullable = false, length = 50)
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParcelStatus status = ParcelStatus.REGISTERED;

    // Dimensions physiques
    @Column(precision = 10, scale = 3)
    private BigDecimal weight;

    @Column(precision = 10, scale = 2)
    private BigDecimal length;

    @Column(precision = 10, scale = 2)
    private BigDecimal width;

    @Column(precision = 10, scale = 2)
    private BigDecimal height;

    // Informations commerciales
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "declared_value", precision = 15, scale = 2)
    private BigDecimal declaredValue;

    @Column(length = 3)
    private String currency;

    // Destinataire spécifique (peut différer du shipment)
    @Column(name = "specific_receiver_name")
    private String specificReceiverName;

    @Column(name = "specific_receiver_phone", length = 50)
    private String specificReceiverPhone;

    @Column(name = "specific_receiver_address")
    private String specificReceiverAddress;

    // Tracking
    @Column(name = "current_location_id")
    private UUID currentLocationId;

    @Column(name = "last_scan_at")
    private Instant lastScanAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Métadonnées
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

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

    // TenantAware
    @Override
    public UUID getAgencyId() {
        return agencyId;
    }

    @Override
    public void setAgencyId(UUID agencyId) {
        this.agencyId = agencyId;
    }
}