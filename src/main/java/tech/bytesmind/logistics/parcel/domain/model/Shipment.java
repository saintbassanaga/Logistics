package tech.bytesmind.logistics.parcel.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import tech.bytesmind.logistics.shared.tenancy.model.TenantAware;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Entité Shipment (envoi groupé).
 * Représente une opération d'envoi groupant plusieurs parcels.
 * Cycle de vie court : OPEN → CONFIRMED.
 * La logique métier est dans ShipmentDomainService.
 */
@Setter
@Getter
@Entity
@Table(name = "shipment")
@SQLDelete(sql = "UPDATE shipment SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Shipment implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Column(name = "shipment_number", unique = true, nullable = false, length = 50)
    private String shipmentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.OPEN;

    /**
     * ID du client qui a créé cet envoi (null si créé par un employé d'agence).
     */
    @Column(name = "customer_id")
    private UUID customerId;

    /**
     * ID du point de collecte où l'envoi sera validé.
     */
    @Column(name = "pickup_location_id")
    private UUID pickupLocationId;

    /**
     * ID de l'employé d'agence qui a validé l'envoi.
     */
    @Column(name = "validated_by_id")
    private UUID validatedById;

    /**
     * Timestamp de la validation.
     */
    @Column(name = "validated_at")
    private Instant validatedAt;

    /**
     * Raison du rejet (si le statut est REJECTED).
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Expéditeur
    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "sender_phone", length = 50)
    private String senderPhone;

    @Column(name = "sender_email")
    private String senderEmail;

    @Column(name = "sender_address_line1", nullable = false)
    private String senderAddressLine1;

    @Column(name = "sender_address_line2")
    private String senderAddressLine2;

    @Column(name = "sender_city", nullable = false)
    private String senderCity;

    @Column(name = "sender_postal_code", nullable = false, length = 20)
    private String senderPostalCode;

    @Column(name = "sender_country", nullable = false, length = 2)
    private String senderCountry;

    // Destinataire
    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_phone", length = 50)
    private String receiverPhone;

    @Column(name = "receiver_email")
    private String receiverEmail;

    @Column(name = "receiver_address_line1", nullable = false)
    private String receiverAddressLine1;

    @Column(name = "receiver_address_line2")
    private String receiverAddressLine2;

    @Column(name = "receiver_city", nullable = false)
    private String receiverCity;

    @Column(name = "receiver_postal_code", nullable = false, length = 20)
    private String receiverPostalCode;

    @Column(name = "receiver_country", nullable = false, length = 2)
    private String receiverCountry;

    // Informations commerciales
    @Column(name = "total_weight", precision = 10, scale = 3)
    private BigDecimal totalWeight;

    @Column(name = "declared_value", precision = 15, scale = 2)
    private BigDecimal declaredValue;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Relations
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<Parcel> parcels = new ArrayList<>();

    // Métadonnées
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

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

    // Business helpers

    /**
     * Vérifie si cet envoi a été créé par un client.
     */
    public boolean isCustomerCreated() {
        return customerId != null;
    }

    /**
     * Vérifie si cet envoi est en attente de validation.
     */
    public boolean isPendingValidation() {
        return status == ShipmentStatus.PENDING_VALIDATION;
    }

    /**
     * Vérifie si cet envoi a été validé.
     */
    public boolean isValidated() {
        return validatedById != null && validatedAt != null;
    }

    /**
     * Vérifie si cet envoi a été rejeté.
     */
    public boolean isRejected() {
        return status == ShipmentStatus.REJECTED;
    }

    /**
     * Vérifie si cet envoi peut recevoir des colis.
     */
    public boolean canAddParcels() {
        return status == ShipmentStatus.OPEN;
    }
}