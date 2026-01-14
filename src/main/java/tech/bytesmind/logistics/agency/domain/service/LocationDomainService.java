package tech.bytesmind.logistics.agency.domain.service;


import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.agency.domain.model.Agency;
import tech.bytesmind.logistics.agency.domain.model.AgencyLocation;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

import java.math.BigDecimal;

/**
 * Service de domaine contenant TOUTE la logique métier pour AgencyLocation.
 */
@Service
public class LocationDomainService {

    /**
     * Valide qu'une agence peut ajouter une localisation.
     */
    public void validateCanAddLocation(Agency agency) {
        if (!agency.isActive()) {
            throw new BusinessException("Cannot add location to inactive agency");
        }

        if (agency.isSuspended()) {
            throw new BusinessException("Cannot add location to suspended agency");
        }
    }

    /**
     * Vérifie si une localisation est opérationnelle.
     */
    public boolean isOperational(AgencyLocation location) {
        return location.isActive() && !location.isTemporarilyClosed();
    }

    /**
     * Vérifie si une localisation a atteint sa capacité journalière.
     */
    public boolean hasReachedDailyCapacity(AgencyLocation location, int currentDailyParcels) {
        Integer maxDaily = location.getMaxDailyParcels();
        return maxDaily != null && currentDailyParcels >= maxDaily;
    }

    /**
     * Ferme temporairement une localisation.
     */
    public void temporaryClose(AgencyLocation location, String reason) {
        if (location.isTemporarilyClosed()) {
            throw new BusinessException("Location is already temporarily closed");
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Closure reason is required");
        }

        location.setTemporarilyClosed(true);
        location.setClosureReason(reason);
    }

    /**
     * Rouvre une localisation.
     */
    public void reopen(AgencyLocation location) {
        if (!location.isTemporarilyClosed()) {
            throw new BusinessException("Location is not temporarily closed");
        }

        location.setTemporarilyClosed(false);
        location.setClosureReason(null);
    }

    /**
     * Désactive une localisation.
     */
    public void deactivate(AgencyLocation location) {
        if (!location.isActive()) {
            throw new BusinessException("Location is already inactive");
        }

        location.setActive(false);
    }

    /**
     * Active une localisation.
     */
    public void activate(AgencyLocation location) {
        if (location.isActive()) {
            throw new BusinessException("Location is already active");
        }

        location.setActive(true);
        location.setTemporarilyClosed(false);
        location.setClosureReason(null);
    }

    /**
     * Calcule la distance en km entre deux localisations (Haversine).
     */
    public Double calculateDistanceKm(AgencyLocation loc1, AgencyLocation loc2) {
        BigDecimal lat1 = loc1.getLatitude();
        BigDecimal lon1 = loc1.getLongitude();
        BigDecimal lat2 = loc2.getLatitude();
        BigDecimal lon2 = loc2.getLongitude();

        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }

        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lon1Rad = Math.toRadians(lon1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double lon2Rad = Math.toRadians(lon2.doubleValue());

        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371.0 * c; // Rayon de la Terre en km
    }
}
