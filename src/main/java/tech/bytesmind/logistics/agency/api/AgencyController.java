package tech.bytesmind.logistics.agency.api;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.bytesmind.logistics.agency.api.dto.AgencyResponse;
import tech.bytesmind.logistics.agency.api.dto.CreateAgencyRequest;
import tech.bytesmind.logistics.agency.api.dto.CreateLocationRequest;
import tech.bytesmind.logistics.agency.api.dto.LocationResponse;
import tech.bytesmind.logistics.agency.application.mapper.AgencyMapper;
import tech.bytesmind.logistics.agency.application.mapper.LocationMapper;
import tech.bytesmind.logistics.agency.application.service.AgencyService;
import tech.bytesmind.logistics.agency.application.service.LocationService;
import tech.bytesmind.logistics.agency.domain.model.Agency;
import tech.bytesmind.logistics.agency.domain.model.AgencyLocation;
import tech.bytesmind.logistics.agency.application.policy.AgencyAccessPolicy;
import tech.bytesmind.logistics.agency.application.policy.LocationAccessPolicy;
import tech.bytesmind.logistics.shared.security.annotations.RequireActor;
import tech.bytesmind.logistics.shared.security.annotations.RequireRole;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.service.SecurityContextService;


import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/agencies")
public class AgencyController {

    private static final Logger log = LoggerFactory.getLogger(AgencyController.class);

    private final AgencyService agencyService;
    private final LocationService locationService;
    private final AgencyMapper agencyMapper;
    private final LocationMapper locationMapper;
    private final SecurityContextService securityContextService;
    private final AgencyAccessPolicy agencyAccessPolicy;
    private final LocationAccessPolicy locationAccessPolicy;

    /**
     * POST /agencies/register
     * Register a new agency. The authenticated user becomes AGENCY_ADMIN.
     * Only CUSTOMER users can register agencies.
     */
    @PostMapping("/register")
    @RequireActor(ActorType.CUSTOMER)
    public ResponseEntity<AgencyResponse> registerAgency(@Valid @RequestBody CreateAgencyRequest request) {
        log.info("POST /agencies/register - Registering agency: {}", request.name());

        var context = securityContextService.getCurrentSecurityContext();
        UUID userId = context.userId();

        Agency agency = agencyService.createAgency(userId, request);
        AgencyResponse response = agencyMapper.toResponse(agency);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/agencies
     * Lister les agences.
     */
    @GetMapping
    public ResponseEntity<List<AgencyResponse>> listAgencies() {
        log.info("GET /agencies - Listing agencies");

        List<Agency> agencies = agencyService.listAllAgencies();
        List<AgencyResponse> responses = agencies.stream()
                .map(agencyMapper::toResponseWithoutLocations)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/agencies/{id}
     * Récupérer une agence avec ses localisations.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AgencyResponse> getAgency(@PathVariable UUID id) {
        log.info("GET /agencies/{}", id);

        var context = securityContextService.getCurrentSecurityContext();
        agencyAccessPolicy.validateAccess(context, id); // Validation ABAC

        Agency agency = agencyService.getAgencyWithLocations(id);
        AgencyResponse response = agencyMapper.toResponse(agency);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /agencies/{agencyId}/suspend
     * Suspendre une agence (Platform Admin uniquement).
     */
    @PostMapping("/{agencyId}/suspend")
    @RequireActor(ActorType.PLATFORM_ADMIN)
    public ResponseEntity<Void> suspendAgency(
            @PathVariable UUID agencyId,
            @RequestParam String reason
    ) {
        log.info("POST /agencies/{}/suspend", agencyId);

        var context = securityContextService.getCurrentSecurityContext();

        if (!agencyAccessPolicy.canSuspend(context)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        agencyService.suspendAgency(agencyId, reason);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /agencies/{agencyId}/unsuspend
     * Réactiver une agence (Platform Admin uniquement).
     */
    @PostMapping("/{agencyId}/unsuspend")
    @RequireActor(ActorType.PLATFORM_ADMIN)
    public ResponseEntity<Void> unsuspendAgency(@PathVariable UUID agencyId) {
        log.info("POST /api/agencies/{}/unsuspend", agencyId);

        agencyService.unsuspendAgency(agencyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /agencies/{agencyId}/locations
     * Ajouter une localisation.
     */
    @PostMapping("/{agencyId}/locations")
    @RequireRole("AGENCY_ADMIN")
    public ResponseEntity<LocationResponse> addLocation(
            @PathVariable UUID agencyId,
            @Valid @RequestBody CreateLocationRequest request
    ) {
        log.info("POST /agencies/{}/locations", agencyId);

        var context = securityContextService.getCurrentSecurityContext();
        locationAccessPolicy.validateAccess(context, agencyId); // Validation ABAC

        AgencyLocation location = locationService.addLocation(agencyId, request);
        LocationResponse response = locationMapper.toResponse(location);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/{agencyId}/locations")
    public ResponseEntity<List<LocationResponse>> listLocations(@PathVariable UUID agencyId) {
        log.info("GET /agencies/{}/locations", agencyId);

        var context = securityContextService.getCurrentSecurityContext();
        locationAccessPolicy.validateAccess(context, agencyId); // Validation ABAC

        List<AgencyLocation> locations = locationService.listLocationsByAgency(agencyId);
        List<LocationResponse> responses = locationMapper.toResponseList(locations);

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /locations/{id}
     * Récupérer une localisation.
     */
    @GetMapping("/locations/{id}")
    public ResponseEntity<LocationResponse> getLocation(@PathVariable UUID id) {
        log.info("GET /locations/{}", id);

        AgencyLocation location = locationService.getLocationById(id);

        var context = securityContextService.getCurrentSecurityContext();
        locationAccessPolicy.validateAccess(context, location.getAgencyId()); // Validation ABAC

        LocationResponse response = locationMapper.toResponse(location);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /locations/{id}/close
     * Fermer temporairement une localisation.
     */
    @PostMapping("/locations/{id}/close")
    @RequireRole("LOCATION_MANAGER")
    public ResponseEntity<Void> closeLocation(
            @PathVariable UUID id,
            @RequestParam String reason
    ) {
        log.info("POST /locations/{}/close", id);

        locationService.temporaryCloseLocation(id, reason);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /locations/{id}/reopen
     * Rouvrir une localisation.
     */
    @PostMapping("/locations/{id}/reopen")
    @RequireRole("LOCATION_MANAGER")
    public ResponseEntity<Void> reopenLocation(@PathVariable UUID id) {
        log.info("POST /locations/{}/reopen", id);

        locationService.reopenLocation(id);
        return ResponseEntity.noContent().build();
    }
}
