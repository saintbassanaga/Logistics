package tech.bytesmind.logistics.parcel.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.bytesmind.logistics.parcel.api.dto.CreateShipmentRequest;
import tech.bytesmind.logistics.parcel.api.dto.ShipmentResponse;
import tech.bytesmind.logistics.parcel.application.mapper.ShipmentMapper;
import tech.bytesmind.logistics.parcel.application.service.ShipmentService;
import tech.bytesmind.logistics.parcel.domain.model.Shipment;
import tech.bytesmind.logistics.parcel.domain.policy.ShipmentAccessPolicy;
import tech.bytesmind.logistics.shared.security.annotations.RequireActor;
import tech.bytesmind.logistics.shared.security.annotations.RequireRole;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.model.SecurityContext;
import tech.bytesmind.logistics.shared.security.service.SecurityContextService;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des Shipments.
 * RBAC via annotations + ABAC via policies.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/shipments")
public class ShipmentController {

    private static final Logger log = LoggerFactory.getLogger(ShipmentController.class);

    private final ShipmentService shipmentService;
    private final ShipmentMapper shipmentMapper;
    private final SecurityContextService securityContextService;
    private final ShipmentAccessPolicy shipmentAccessPolicy;

    /**
     * POST /shipments
     * Créer un nouveau Shipment.
     */
    @PostMapping
    @RequireActor(ActorType.AGENCY_EMPLOYEE)
    public ResponseEntity<ShipmentResponse> createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        log.info("POST /shipments - Creating shipment");

        SecurityContext context = securityContextService.getCurrentSecurityContext();

        if (!shipmentAccessPolicy.canCreate(context)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Shipment shipment = shipmentMapper.toEntity(request);
        Shipment created = shipmentService.createShipment(context.agencyId(), shipment);
        ShipmentResponse response = shipmentMapper.toResponseWithoutParcels(created);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /shipments
     * Lister les Shipments de l'agence de l'employé connecté.
     */
    @GetMapping
    @RequireActor(ActorType.AGENCY_EMPLOYEE)
    public ResponseEntity<List<ShipmentResponse>> listShipments() {
        log.info("GET /shipments - Listing shipments");

        SecurityContext context = securityContextService.getCurrentSecurityContext();
        List<Shipment> shipments = shipmentService.listShipmentsByAgency(context.agencyId());
        List<ShipmentResponse> responses = shipmentMapper.toResponseList(shipments);

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /shipments/open
     * Lister les Shipments OPEN.
     */
    @GetMapping("/open")
    @RequireActor(ActorType.AGENCY_EMPLOYEE)
    public ResponseEntity<List<ShipmentResponse>> listOpenShipments() {
        log.info("GET /shipments/open - Listing open shipments");

        SecurityContext context = securityContextService.getCurrentSecurityContext();
        List<Shipment> shipments = shipmentService.listOpenShipments(context.agencyId());
        List<ShipmentResponse> responses = shipmentMapper.toResponseList(shipments);

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /shipments/{id}
     * Récupérer un Shipment avec ses parcels.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipment(@PathVariable UUID id) {
        log.info("GET /shipments/{}", id);

        Shipment shipment = shipmentService.getShipmentWithParcels(id);

        SecurityContext context = securityContextService.getCurrentSecurityContext();
        shipmentAccessPolicy.validateAccess(context, shipment.getAgencyId());

        ShipmentResponse response = shipmentMapper.toResponse(shipment);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /shipments/{id}/confirm
     * Confirmer un Shipment (OPEN → CONFIRMED).
     */
    @PostMapping("/{id}/confirm")
    @RequireRole("SHIPMENT_MANAGER")
    public ResponseEntity<Void> confirmShipment(@PathVariable UUID id) {
        log.info("POST /shipments/{}/confirm", id);

        Shipment shipment = shipmentService.getShipmentById(id);

        SecurityContext context = securityContextService.getCurrentSecurityContext();
        shipmentAccessPolicy.validateConfirm(context, shipment);

        shipmentService.confirmShipment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /shipments/{id}
     * Mettre à jour un Shipment (uniquement si OPEN).
     */
    @PutMapping("/{id}")
    @RequireRole("SHIPMENT_MANAGER")
    public ResponseEntity<ShipmentResponse> updateShipment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateShipmentRequest request
    ) {
        log.info("PUT /shipments/{}", id);

        Shipment existing = shipmentService.getShipmentById(id);

        SecurityContext context = securityContextService.getCurrentSecurityContext();
        shipmentAccessPolicy.validateModify(context, existing);

        Shipment updatedData = shipmentMapper.toEntity(request);
        Shipment updated = shipmentService.updateShipment(id, updatedData);
        ShipmentResponse response = shipmentMapper.toResponseWithoutParcels(updated);

        return ResponseEntity.ok(response);
    }
}
