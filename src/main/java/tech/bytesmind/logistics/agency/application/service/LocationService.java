package tech.bytesmind.logistics.agency.application.service;

import tech.bytesmind.logistics.agency.api.dto.CreateLocationRequest;
import tech.bytesmind.logistics.agency.domain.model.AgencyLocation;

import java.util.List;
import java.util.UUID;

public interface LocationService {
    AgencyLocation addLocation(UUID agencyId, CreateLocationRequest request);

    AgencyLocation getLocationById(UUID id);

    List<AgencyLocation> listLocationsByAgency(UUID agencyId);

    List<AgencyLocation> listActiveLocationsByAgency(UUID agencyId);

    List<AgencyLocation> listOperationalLocationsByAgency(UUID agencyId);

    void temporaryCloseLocation(UUID locationId, String reason);

    void reopenLocation(UUID locationId);

    Double calculateDistance(UUID locationId1, UUID locationId2);
}
