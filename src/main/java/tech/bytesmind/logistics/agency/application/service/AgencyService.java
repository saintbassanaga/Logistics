package tech.bytesmind.logistics.agency.application.service;


import tech.bytesmind.logistics.agency.api.dto.CreateAgencyRequest;
import tech.bytesmind.logistics.agency.domain.model.Agency;

import java.util.List;
import java.util.UUID;

public interface AgencyService {
    Agency createAgency(CreateAgencyRequest request);

    Agency getAgencyById(UUID id);

    Agency getAgencyWithLocations(UUID id);

    List<Agency> listAllAgencies();

    List<Agency> listActiveAgencies();

    void suspendAgency(UUID agencyId, String reason);

    void unsuspendAgency(UUID agencyId);
}
