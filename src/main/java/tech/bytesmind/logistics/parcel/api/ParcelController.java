package tech.bytesmind.logistics.parcel.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.bytesmind.logistics.parcel.api.dto.CreateParcelRequest;
import tech.bytesmind.logistics.parcel.api.dto.ParcelResponse;
import tech.bytesmind.logistics.parcel.api.dto.UpdateParcelStatusRequest;
import tech.bytesmind.logistics.parcel.application.mapper.ParcelMapper;
import tech.bytesmind.logistics.parcel.application.service.ParcelService;
import tech.bytesmind.logistics.parcel.domain.model.Parcel;
import tech.bytesmind.logistics.parcel.domain.policy.ParcelAccessPolicy;
import tech.bytesmind.logistics.shared.security.annotations.RequireActor;
import tech.bytesmind.logistics.shared.security.annotations.RequireRole;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.model.SecurityContext;
import tech.bytesmind.logistics.shared.security.service.SecurityContextService;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des Parcels.
 * RBAC via annotations + ABAC via policies.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/parcels")
public class ParcelController {

    private static final Logger log = LoggerFactory.getLogger(ParcelController.class);

    private final ParcelService parcelService;
    private final ParcelMapper parcelMapper;
    private final SecurityContextService securityContextService;
    private final ParcelAccessPolicy parcelAccessPolicy;

    /**
     * POST /shipments/{shipmentId}/parcels
     * Créer un nouveau Parcel dans un Shipment.
     */
    @PostMapping("/shipments/{shipmentId}/parcels")
    @RequireActor(ActorType.AGENCY_EMPLOYEE)
    public ResponseEntity<ParcelResponse> createParcel(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody CreateParcelRequest request
    ) {
        log.info("POST /shipments/{}/parcels - Creating parcel", shipmentId);

        SecurityContext context = securityContextService.getCurrentSecurityContext();

        if (!parcelAccessPolicy.canCreate(context)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Parcel parcel = parcelMapper.toEntity(request);
        Parcel created = parcelService.createParcel(shipmentId, parcel);
        ParcelResponse response = parcelMapper.toResponse(created);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /parcels/{id}
     * Récupérer un Parcel par son ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ParcelResponse> getParcel(@PathVariable UUID id) {
        log.info("GET /parcels/{}", id);

        Parcel parcel = parcelService.getParcelById(id);

        SecurityContext context = securityContextService.getCurrentSecurityContext();
        parcelAccessPolicy.validateAccess(context, parcel.getAgencyId());

        ParcelResponse response = parcelMapper.toResponse(parcel);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /parcels/tracking/{trackingNumber}
     * Récupérer un Parcel par son numéro de tracking.
     */
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ParcelResponse> getParcelByTracking(@PathVariable String trackingNumber) {
        log.info("GET /parcels/tracking/{}", trackingNumber);

        Parcel parcel = parcelService.getParcelByTrackingNumber(trackingNumber);

        SecurityContext context = securityContextService.getCurrentSecurityContext();
        parcelAccessPolicy.validateAccess(context, parcel.getAgencyId());

        ParcelResponse response = parcelMapper.toResponse(parcel);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /shipments/{shipmentId}/parcels
     * Lister les Parcels d'un Shipment.
     */
    @GetMapping("/shipments/{shipmentId}/parcels")
    public ResponseEntity<List<ParcelResponse>> listParcelsByShipment(@PathVariable UUID shipmentId) {
        log.info("GET /shipments/{}/parcels", shipmentId);

        List<Parcel> parcels = parcelService.listParcelsByShipment(shipmentId);

        // Validation ABAC sur le premier parcel (tous ont le même agencyId via shipment)
        if (!parcels.isEmpty()) {
            SecurityContext context = securityContextService.getCurrentSecurityContext();
            parcelAccessPolicy.validateAccess(context, parcels.get(0).getAgencyId());
        }

        List<ParcelResponse> responses = parcelMapper.toResponseList(parcels);
        return ResponseEntity.ok(responses);
    }

    /**
     * GET /parcels/active
     * Lister les Parcels actifs de l'agence.
     */
    @GetMapping("/active")
    @RequireActor(ActorType.AGENCY_EMPLOYEE)
    public ResponseEntity<List<ParcelResponse>> listActiveParcels() {
        log.info("GET /parcels/active - Listing active parcels");

        SecurityContext context = securityContextService.getCurrentSecurityContext();
        List<Parcel> parcels = parcelService.listActiveParcels(context.agencyId());
        List<ParcelResponse> responses = parcelMapper.toResponseList(parcels);

        return ResponseEntity.ok(responses);
    }

    /**
     * PUT /parcels/{id}/status
     * Mettre à jour le statut d'un Parcel.
     */
    @PutMapping("/{id}/status")
    @RequireRole("PARCEL_MANAGER")
    public ResponseEntity<Void> updateParcelStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateParcelStatusRequest request
    ) {
        log.info("PUT /parcels/{}/status -> {}", id, request.newStatus());

        Parcel parcel = parcelService.getParcelById(id);

        SecurityContext context = securityContextService.getCurrentSecurityContext();
        parcelAccessPolicy.validateUpdateStatus(context, parcel);

        parcelService.updateParcelStatus(id, request.newStatus(), request.locationId());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /parcels/{id}/deliver
     * Marquer un Parcel comme livré.
     */
    @PostMapping("/{id}/deliver")
    @RequireRole("DELIVERY_DRIVER")
    public ResponseEntity<Void> markParcelAsDelivered(
            @PathVariable UUID id,
            @RequestParam String receivedBy
    ) {
        log.info("POST /parcels/{}/deliver", id);

        Parcel parcel = parcelService.getParcelById(id);

        SecurityContext context = securityContextService.getCurrentSecurityContext();
        parcelAccessPolicy.validateUpdateStatus(context, parcel);

        parcelService.markParcelAsDelivered(id, receivedBy);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /parcels/{id}/fail
     * Marquer un Parcel comme échec de livraison.
     */
    @PostMapping("/{id}/fail")
    @RequireRole("DELIVERY_DRIVER")
    public ResponseEntity<Void> markParcelAsFailed(
            @PathVariable UUID id,
            @RequestParam String reason
    ) {
        log.info("POST /parcels/{}/fail", id);

        Parcel parcel = parcelService.getParcelById(id);

        SecurityContext context = securityContextService.getCurrentSecurityContext();
        parcelAccessPolicy.validateUpdateStatus(context, parcel);

        parcelService.markParcelAsFailed(id, reason);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /parcels/{id}
     * Mettre à jour un Parcel (uniquement si REGISTERED).
     */
    @PutMapping("/{id}")
    @RequireRole("PARCEL_MANAGER")
    public ResponseEntity<ParcelResponse> updateParcel(
            @PathVariable UUID id,
            @Valid @RequestBody CreateParcelRequest request
    ) {
        log.info("PUT /parcels/{}", id);

        Parcel existing = parcelService.getParcelById(id);

        SecurityContext context = securityContextService.getCurrentSecurityContext();
        parcelAccessPolicy.validateModify(context, existing);

        Parcel updatedData = parcelMapper.toEntity(request);
        Parcel updated = parcelService.updateParcel(id, updatedData);
        ParcelResponse response = parcelMapper.toResponse(updated);

        return ResponseEntity.ok(response);
    }
}
