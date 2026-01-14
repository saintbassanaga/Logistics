package tech.bytesmind.logistics.parcel.domain.service;

import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.parcel.domain.model.Parcel;
import tech.bytesmind.logistics.parcel.domain.model.ParcelStatus;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

import java.time.Instant;
import java.util.UUID;

/**
 * Service de domaine pour la logique métier des Parcels.
 * Implémente les invariants et règles métier du tracking.
 */
@Service
public class ParcelDomainService {

    /**
     * Change le statut d'un Parcel.
     * Valide les transitions autorisées.
     */
    public void changeStatus(Parcel parcel, ParcelStatus newStatus, UUID locationId) {
        ParcelStatus currentStatus = parcel.getStatus();

        if (currentStatus == newStatus) {
            throw new BusinessException("Parcel is already in status " + newStatus);
        }

        validateStatusTransition(currentStatus, newStatus);

        parcel.setStatus(newStatus);
        parcel.setCurrentLocationId(locationId);
        parcel.setLastScanAt(Instant.now());

        if (newStatus == ParcelStatus.DELIVERED) {
            parcel.setDeliveredAt(Instant.now());
        }
    }

    /**
     * Valide une transition de statut.
     */
    private void validateStatusTransition(ParcelStatus from, ParcelStatus to) {
        boolean isValid = switch (from) {
            case REGISTERED -> to == ParcelStatus.IN_TRANSIT || to == ParcelStatus.IN_SORTING;
            case IN_TRANSIT -> to == ParcelStatus.IN_SORTING || to == ParcelStatus.OUT_FOR_DELIVERY ||
                    to == ParcelStatus.FAILED;
            case IN_SORTING -> to == ParcelStatus.IN_TRANSIT || to == ParcelStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> to == ParcelStatus.DELIVERED || to == ParcelStatus.FAILED ||
                    to == ParcelStatus.IN_TRANSIT;
            case DELIVERED -> false; // État terminal
            case FAILED -> to == ParcelStatus.RETURNED || to == ParcelStatus.IN_TRANSIT;
            case RETURNED -> false; // État terminal
        };

        if (!isValid) {
            throw new BusinessException(
                    String.format("Invalid status transition: %s → %s", from, to)
            );
        }
    }

    /**
     * Vérifie si un Parcel est dans un état terminal.
     */
    public boolean isInTerminalState(Parcel parcel) {
        return parcel.getStatus() == ParcelStatus.DELIVERED ||
                parcel.getStatus() == ParcelStatus.RETURNED;
    }

    /**
     * Vérifie si un Parcel peut être modifié.
     */
    public boolean isModifiable(Parcel parcel) {
        return parcel.getStatus() == ParcelStatus.REGISTERED;
    }

    /**
     * Valide qu'un Parcel peut être modifié.
     */
    public void validateModifiable(Parcel parcel) {
        if (!isModifiable(parcel)) {
            throw new BusinessException("Cannot modify parcel in status " + parcel.getStatus());
        }
    }

    /**
     * Valide les données d'un Parcel avant création.
     */
    public void validateParcelData(Parcel parcel) {
        if (parcel.getWeight() == null || parcel.getWeight().signum() <= 0) {
            throw new BusinessException("Parcel weight must be positive");
        }

        if (parcel.getDescription() == null || parcel.getDescription().isBlank()) {
            throw new BusinessException("Parcel description is required");
        }
    }

    /**
     * Génère un numéro de tracking unique.
     */
    public String generateTrackingNumber(String agencyCode) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf((int) (Math.random() * 100000));
        return String.format("TRK-%s-%s-%s", agencyCode, timestamp, random);
    }

    /**
     * Marque un Parcel comme livré.
     */
    public void markAsDelivered(Parcel parcel, String receivedBy) {
        if (parcel.getStatus() != ParcelStatus.OUT_FOR_DELIVERY) {
            throw new BusinessException("Only OUT_FOR_DELIVERY parcels can be marked as delivered");
        }

        parcel.setStatus(ParcelStatus.DELIVERED);
        parcel.setDeliveredAt(Instant.now());
        parcel.setLastScanAt(Instant.now());
    }

    /**
     * Marque un Parcel comme échec de livraison.
     */
    public void markAsFailed(Parcel parcel, String reason) {
        if (isInTerminalState(parcel)) {
            throw new BusinessException("Cannot mark terminal state parcel as failed");
        }

        parcel.setStatus(ParcelStatus.FAILED);
        parcel.setLastScanAt(Instant.now());

        if (parcel.getNotes() == null) {
            parcel.setNotes("Failed: " + reason);
        } else {
            parcel.setNotes(parcel.getNotes() + "\nFailed: " + reason);
        }
    }
}