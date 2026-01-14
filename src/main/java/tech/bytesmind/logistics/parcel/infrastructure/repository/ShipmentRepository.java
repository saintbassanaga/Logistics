package tech.bytesmind.logistics.parcel.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.bytesmind.logistics.parcel.domain.model.Shipment;
import tech.bytesmind.logistics.parcel.domain.model.ShipmentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'entité Shipment.
 * Toutes les requêtes doivent respecter l'isolation multi-tenant via agency_id.
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByShipmentNumber(String shipmentNumber);

    boolean existsByShipmentNumber(String shipmentNumber);

    List<Shipment> findByAgencyId(UUID agencyId);

    List<Shipment> findByAgencyIdAndStatus(UUID agencyId, ShipmentStatus status);

    @Query("SELECT s FROM Shipment s WHERE s.agencyId = :agencyId AND s.status = 'OPEN'")
    List<Shipment> findOpenShipmentsByAgency(@Param("agencyId") UUID agencyId);

    @Query("SELECT s FROM Shipment s WHERE s.agencyId = :agencyId AND s.status = 'CONFIRMED'")
    List<Shipment> findConfirmedShipmentsByAgency(@Param("agencyId") UUID agencyId);

    @Query("SELECT s FROM Shipment s LEFT JOIN FETCH s.parcels WHERE s.id = :id")
    Optional<Shipment> findByIdWithParcels(@Param("id") UUID id);

    @Query("SELECT s FROM Shipment s LEFT JOIN FETCH s.parcels WHERE s.id = :id AND s.agencyId = :agencyId")
    Optional<Shipment> findByIdAndAgencyIdWithParcels(@Param("id") UUID id, @Param("agencyId") UUID agencyId);

    Optional<Shipment> findByIdAndAgencyId(UUID id, UUID agencyId);

    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.agencyId = :agencyId AND s.status = :status")
    long countByAgencyIdAndStatus(@Param("agencyId") UUID agencyId, @Param("status") ShipmentStatus status);

    /**
     * Compte les shipments d'une agence dont le numéro commence par un préfixe donné.
     * Utilisé par ShipmentNumberGenerator pour la génération séquentielle.
     */
    long countByAgencyIdAndShipmentNumberStartingWith(UUID agencyId, String prefix);
}