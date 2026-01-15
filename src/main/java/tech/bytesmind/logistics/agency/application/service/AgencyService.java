package tech.bytesmind.logistics.agency.application.service;


import tech.bytesmind.logistics.agency.api.dto.CreateAgencyRequest;
import tech.bytesmind.logistics.agency.domain.model.Agency;

import java.util.List;
import java.util.UUID;

public interface AgencyService {

    /**
     * Crée une nouvelle agence.
     * L'utilisateur devient automatiquement AGENCY_ADMIN de l'agence créée.
     * Son actorType passe de CUSTOMER à AGENCY_EMPLOYEE.
     *
     * @param userId  ID de l'utilisateur qui crée l'agence
     * @param request Données de l'agence
     * @return L'agence créée
     */
    Agency createAgency(UUID userId, CreateAgencyRequest request);

    Agency getAgencyById(UUID id);

    Agency getAgencyWithLocations(UUID id);

    List<Agency> listAllAgencies();

    List<Agency> listActiveAgencies();

    void suspendAgency(UUID agencyId, String reason);

    void unsuspendAgency(UUID agencyId);
}
