package tech.bytesmind.logistics.parcel.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.bytesmind.logistics.parcel.api.dto.RejectShipmentRequest;
import tech.bytesmind.logistics.parcel.api.dto.ShipmentResponse;
import tech.bytesmind.logistics.parcel.api.dto.ValidateShipmentRequest;
import tech.bytesmind.logistics.parcel.application.mapper.ShipmentMapper;
import tech.bytesmind.logistics.parcel.application.service.ShipmentValidationService;
import tech.bytesmind.logistics.parcel.domain.model.Shipment;
import tech.bytesmind.logistics.shared.security.annotations.RequireRole;
import tech.bytesmind.logistics.shared.security.service.SecurityContextService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller pour la validation des envois clients par les employés d'agence.
 * Permet de valider ou rejeter les envois en statut PENDING_VALIDATION.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/shipments/validation")
@Tag(name = "Shipment Validation", description = "Endpoints for agency employees to validate customer shipments")
@SecurityRequirement(name = "bearer-jwt")
public class ShipmentValidationController {

    private static final Logger log = LoggerFactory.getLogger(ShipmentValidationController.class);

    private final ShipmentValidationService shipmentValidationService;
    private final ShipmentMapper shipmentMapper;
    private final SecurityContextService securityContextService;

    /**
     * GET /shipments/validation/pending
     * Liste les envois en attente de validation pour l'agence de l'employé.
     */
    @GetMapping("/pending")
    @RequireRole({"SHIPMENT_MANAGER", "SHIPMENT_CLERK", "AGENCY_ADMIN", "AGENCY_MANAGER"})
    @Operation(
            summary = "List pending validation shipments",
            description = "Lists all shipments with PENDING_VALIDATION status for the employee's agency."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    public ResponseEntity<List<ShipmentResponse>> listPendingValidation(
            @RequestParam(required = false) UUID locationId) {
        log.info("GET /shipments/validation/pending - locationId: {}", locationId);

        var context = securityContextService.getCurrentSecurityContext();

        List<Shipment> shipments;
        if (locationId != null) {
            shipments = shipmentValidationService.listPendingValidationByLocation(locationId);
        } else {
            shipments = shipmentValidationService.listPendingValidation(context.agencyId());
        }

        return ResponseEntity.ok(shipmentMapper.toResponseList(shipments));
    }

    /**
     * GET /shipments/validation/pending/count
     * Compte les envois en attente de validation.
     */
    @GetMapping("/pending/count")
    @RequireRole({"SHIPMENT_MANAGER", "SHIPMENT_CLERK", "AGENCY_ADMIN", "AGENCY_MANAGER"})
    @Operation(
            summary = "Count pending validation shipments",
            description = "Returns the count of shipments pending validation."
    )
    public ResponseEntity<Map<String, Long>> countPendingValidation(
            @RequestParam(required = false) UUID locationId) {
        log.info("GET /shipments/validation/pending/count - locationId: {}", locationId);

        var context = securityContextService.getCurrentSecurityContext();

        long count;
        if (locationId != null) {
            count = shipmentValidationService.countPendingValidationByLocation(locationId);
        } else {
            count = shipmentValidationService.countPendingValidation(context.agencyId());
        }

        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * GET /shipments/validation/{id}
     * Récupère un envoi en attente de validation.
     */
    @GetMapping("/{id}")
    @RequireRole({"SHIPMENT_MANAGER", "SHIPMENT_CLERK", "AGENCY_ADMIN", "AGENCY_MANAGER"})
    @Operation(
            summary = "Get pending shipment",
            description = "Retrieves a shipment pending validation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ShipmentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Shipment not found or not pending", content = @Content)
    })
    public ResponseEntity<ShipmentResponse> getPendingShipment(@PathVariable UUID id) {
        log.info("GET /shipments/validation/{}", id);

        var context = securityContextService.getCurrentSecurityContext();
        Shipment shipment = shipmentValidationService.getPendingShipment(context.agencyId(), id);

        return ResponseEntity.ok(shipmentMapper.toResponse(shipment));
    }

    /**
     * POST /shipments/validation/{id}/validate
     * Valide un envoi client.
     */
    @PostMapping("/{id}/validate")
    @RequireRole({"SHIPMENT_MANAGER", "SHIPMENT_CLERK", "AGENCY_ADMIN", "AGENCY_MANAGER"})
    @Operation(
            summary = "Validate shipment",
            description = "Validates a customer shipment. Changes status from PENDING_VALIDATION to OPEN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment validated successfully",
                    content = @Content(schema = @Schema(implementation = ShipmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Shipment cannot be validated", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<ShipmentResponse> validateShipment(
            @PathVariable UUID id,
            @RequestBody(required = false) ValidateShipmentRequest request) {
        log.info("POST /shipments/validation/{}/validate", id);

        var context = securityContextService.getCurrentSecurityContext();
        String notes = request != null ? request.notes() : null;

        Shipment shipment = shipmentValidationService.validateShipment(
                context.agencyId(),
                id,
                context.userId(),
                notes
        );

        return ResponseEntity.ok(shipmentMapper.toResponseWithoutParcels(shipment));
    }

    /**
     * POST /shipments/validation/{id}/reject
     * Rejette un envoi client.
     */
    @PostMapping("/{id}/reject")
    @RequireRole({"SHIPMENT_MANAGER", "SHIPMENT_CLERK", "AGENCY_ADMIN", "AGENCY_MANAGER"})
    @Operation(
            summary = "Reject shipment",
            description = "Rejects a customer shipment. Changes status from PENDING_VALIDATION to REJECTED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment rejected successfully",
                    content = @Content(schema = @Schema(implementation = ShipmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or shipment cannot be rejected", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<ShipmentResponse> rejectShipment(
            @PathVariable UUID id,
            @Valid @RequestBody RejectShipmentRequest request) {
        log.info("POST /shipments/validation/{}/reject", id);

        var context = securityContextService.getCurrentSecurityContext();

        Shipment shipment = shipmentValidationService.rejectShipment(
                context.agencyId(),
                id,
                context.userId(),
                request.reason()
        );

        return ResponseEntity.ok(shipmentMapper.toResponseWithoutParcels(shipment));
    }
}