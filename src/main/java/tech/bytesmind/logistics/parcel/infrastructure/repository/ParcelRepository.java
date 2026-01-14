package tech.bytesmind.logistics.parcel.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.bytesmind.logistics.parcel.domain.model.Parcel;
import tech.bytesmind.logistics.parcel.domain.model.ParcelStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'entité Parcel.
 * Toutes les requêtes doivent respecter l'isolation multi-tenant via agency_id.
 */
@Repository
public interface ParcelRepository extends JpaRepository<Parcel, UUID> {

    Optional<Parcel> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);

    List<Parcel> findByAgencyId(UUID agencyId);

    List<Parcel> findByAgencyIdAndStatus(UUID agencyId, ParcelStatus status);

    List<Parcel> findByShipmentId(UUID shipmentId);

    @Query("SELECT p FROM Parcel p WHERE p.agencyId = :agencyId AND p.shipment.id = :shipmentId")
    List<Parcel> findByAgencyIdAndShipmentId(@Param("agencyId") UUID agencyId, @Param("shipmentId") UUID shipmentId);

    Optional<Parcel> findByIdAndAgencyId(UUID id, UUID agencyId);

    Optional<Parcel> findByTrackingNumberAndAgencyId(String trackingNumber, UUID agencyId);

    @Query("SELECT p FROM Parcel p WHERE p.currentLocationId = :locationId AND p.status IN :statuses")
    List<Parcel> findByCurrentLocationIdAndStatusIn(
            @Param("locationId") UUID locationId,
            @Param("statuses") List<ParcelStatus> statuses
    );

    @Query("SELECT COUNT(p) FROM Parcel p WHERE p.agencyId = :agencyId AND p.status = :status")
    long countByAgencyIdAndStatus(@Param("agencyId") UUID agencyId, @Param("status") ParcelStatus status);

    @Query("SELECT p FROM Parcel p WHERE p.agencyId = :agencyId AND p.status IN ('IN_TRANSIT', 'IN_SORTING', 'OUT_FOR_DELIVERY')")
    List<Parcel> findActiveParcelsByAgency(@Param("agencyId") UUID agencyId);

    @Query("SELECT p FROM Parcel p WHERE p.agencyId = :agencyId AND p.status = 'DELIVERED' AND p.deliveredAt >= CURRENT_DATE")
    List<Parcel> findTodayDeliveriesByAgency(@Param("agencyId") UUID agencyId);
}