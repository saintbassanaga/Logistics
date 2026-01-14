package tech.bytesmind.logistics.agency.domain.service;


import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.agency.domain.model.Agency;
import tech.bytesmind.logistics.agency.domain.model.AgencyLocation;
import tech.bytesmind.logistics.agency.domain.model.LocationType;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

import java.util.List;


@Service
public class AgencyDomainService {

    /**
     * Vérifie si une agence a au moins un siège social actif.
     */
    public boolean hasActiveHeadquarters(Agency agency) {
        if (agency.getAgencyLocations() == null) {
            return false;
        }

        return agency.getAgencyLocations().stream()
                .anyMatch(loc -> loc.getLocationType() == LocationType.HEADQUARTERS
                        && loc.isActive());
    }

    /**
     * Compte le nombre de localisations actives.
     */
    public long countActiveLocations(Agency agency) {
        if (agency.getAgencyLocations() == null) {
            return 0;
        }

        return agency.getAgencyLocations().stream()
                .filter(AgencyLocation::isActive)
                .count();
    }

    /**
     * Récupère les localisations opérationnelles.
     */
    public List<AgencyLocation> getOperationalLocations(Agency agency) {
        if (agency.getAgencyLocations() == null) {
            return List.of();
        }

        return agency.getAgencyLocations().stream()
                .filter(loc -> loc.isActive() && !loc.isTemporarilyClosed())
                .toList();
    }

    /**
     * Vérifie si une agence peut effectuer des opérations.
     */
    public boolean canPerformOperations(Agency agency) {
        return agency.isActive() && !agency.isSuspended();
    }

    /**
     * Vérifie si une agence a atteint sa limite d'utilisateurs.
     */
    public boolean hasReachedUserLimit(Agency agency, int currentUserCount) {
        Integer maxUsers = agency.getMaxUsers();
        return maxUsers != null && currentUserCount >= maxUsers;
    }

    /**
     * Vérifie si une agence a atteint sa limite d'envois.
     */
    public boolean hasReachedShipmentLimit(Agency agency, int currentMonthShipments) {
        Integer maxShipments = agency.getMaxShipmentsPerMonth();
        return maxShipments != null && currentMonthShipments >= maxShipments;
    }

    /**
     * Suspend une agence.
     */
    public void suspend(Agency agency, String reason) {
        if (agency.isSuspended()) {
            throw new BusinessException("Agency is already suspended");
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Suspension reason is required");
        }

        agency.setSuspended(true);
        agency.setSuspensionReason(reason);
    }

    /**
     * Réactive une agence suspendue.
     */
    public void unsuspend(Agency agency) {
        if (!agency.isSuspended()) {
            throw new BusinessException("Agency is not suspended");
        }

        agency.setSuspended(false);
        agency.setSuspensionReason(null);
    }

    /**
     * Désactive une agence.
     */
    public void deactivate(Agency agency) {
        if (!agency.isActive()) {
            throw new BusinessException("Agency is already inactive");
        }

        agency.setActive(false);
    }

    /**
     * Active une agence.
     */
    public void activate(Agency agency) {
        if (agency.isActive()) {
            throw new BusinessException("Agency is already active");
        }

        agency.setActive(true);
        agency.setSuspended(false);
        agency.setSuspensionReason(null);
    }

    /**
     * Valide qu'une agence peut créer un nouvel envoi.
     */
    public void validateCanCreateShipment(Agency agency, int currentMonthShipments) {
        if (!canPerformOperations(agency)) {
            throw new BusinessException("Agency cannot perform operations (inactive or suspended)");
        }

        if (hasReachedShipmentLimit(agency, currentMonthShipments)) {
            throw new BusinessException("Agency has reached monthly shipment limit");
        }
    }
}
