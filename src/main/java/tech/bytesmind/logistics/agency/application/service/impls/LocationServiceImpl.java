package tech.bytesmind.logistics.agency.application.service.impls;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.agency.api.dto.CreateLocationRequest;
import tech.bytesmind.logistics.agency.application.mapper.LocationMapper;
import tech.bytesmind.logistics.agency.application.service.LocationService;
import tech.bytesmind.logistics.agency.domain.event.AgencyLocationAddedEvent;
import tech.bytesmind.logistics.agency.domain.model.Agency;
import tech.bytesmind.logistics.agency.domain.model.AgencyLocation;
import tech.bytesmind.logistics.agency.domain.service.LocationDomainService;
import tech.bytesmind.logistics.agency.infrastructure.repository.AgencyLocationRepository;
import tech.bytesmind.logistics.agency.infrastructure.repository.AgencyRepository;
import tech.bytesmind.logistics.shared.event.publisher.TransactionalEventPublisher;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;
import tech.bytesmind.logistics.shared.tenancy.service.TenantService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationServiceImpl.class);

    private final AgencyRepository agencyRepository;
    private final AgencyLocationRepository locationRepository;
    private final LocationDomainService domainService;
    private final LocationMapper locationMapper;
    private final TenantService tenantService;
    private final TransactionalEventPublisher eventPublisher;

    /**
     * Adds location to agency; publishes creation event
     */
    @Override
    @Transactional
    public AgencyLocation addLocation(UUID agencyId, CreateLocationRequest request) {
        log.info("Adding location '{}' to agency {}", request.code(), agencyId);

        tenantService.validateAgencyAccess(agencyId);

        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new BusinessException("Agency not found: " + agencyId));

        domainService.validateCanAddLocation(agency);

        if (locationRepository.existsByAgencyIdAndCode(agencyId, request.code())) {
            throw new BusinessException("Location code '" + request.code() + "' already exists");
        }

        AgencyLocation location = locationMapper.toEntity(request);
        location.setAgency(agency);
        location.setAgencyId(agencyId);

        location = locationRepository.save(location);

        eventPublisher.publish(new AgencyLocationAddedEvent(
                agencyId,
                location.getId(),
                location.getCode(),
                location.getCity()
        ));

        log.info("Location added successfully: {}", location.getId());
        return location;
    }

    @Override
    @Transactional(readOnly = true)
    public AgencyLocation getLocationById(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Location not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgencyLocation> listLocationsByAgency(UUID agencyId) {
        tenantService.validateAgencyAccess(agencyId);
        return locationRepository.findByAgencyId(agencyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgencyLocation> listActiveLocationsByAgency(UUID agencyId) {
        tenantService.validateAgencyAccess(agencyId);
        return locationRepository.findByAgencyIdAndActiveTrue(agencyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgencyLocation> listOperationalLocationsByAgency(UUID agencyId) {
        tenantService.validateAgencyAccess(agencyId);
        return locationRepository.findOperationalByAgencyId(agencyId);
    }

    @Override
    @Transactional
    public void temporaryCloseLocation(UUID locationId, String reason) {
        log.info("Temporarily closing location: {}", locationId);

        AgencyLocation location = getLocationById(locationId);
        tenantService.validateAgencyAccess(location.getAgencyId());

        domainService.temporaryClose(location, reason);
        locationRepository.save(location);
    }

    @Override
    @Transactional
    public void reopenLocation(UUID locationId) {
        log.info("Reopening location: {}", locationId);

        AgencyLocation location = getLocationById(locationId);
        tenantService.validateAgencyAccess(location.getAgencyId());

        domainService.reopen(location);
        locationRepository.save(location);
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateDistance(UUID locationId1, UUID locationId2) {
        AgencyLocation loc1 = getLocationById(locationId1);
        AgencyLocation loc2 = getLocationById(locationId2);

        return domainService.calculateDistanceKm(loc1, loc2);
    }
}
