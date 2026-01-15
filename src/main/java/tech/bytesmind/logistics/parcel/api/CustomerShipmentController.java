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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.bytesmind.logistics.parcel.api.dto.CustomerShipmentRequest;
import tech.bytesmind.logistics.parcel.api.dto.ShipmentResponse;
import tech.bytesmind.logistics.parcel.application.mapper.ShipmentMapper;
import tech.bytesmind.logistics.parcel.application.service.CustomerShipmentService;
import tech.bytesmind.logistics.parcel.domain.model.Shipment;
import tech.bytesmind.logistics.shared.security.annotations.RequireActor;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.service.SecurityContextService;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller pour la gestion des envois par les clients.
 * Les envois créés par les clients sont initialement en statut PENDING_VALIDATION.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/customer/shipments")
@Tag(name = "Customer Shipments", description = "Endpoints for customers to manage their shipments")
@SecurityRequirement(name = "bearer-jwt")
public class CustomerShipmentController {

    private static final Logger log = LoggerFactory.getLogger(CustomerShipmentController.class);

    private final CustomerShipmentService customerShipmentService;
    private final ShipmentMapper shipmentMapper;
    private final SecurityContextService securityContextService;

    /**
     * POST /customer/shipments
     * Crée un nouvel envoi pour le client connecté.
     * L'envoi est créé avec le statut PENDING_VALIDATION.
     */
    @PostMapping
    @RequireActor(ActorType.CUSTOMER)
    @Operation(
            summary = "Create a new shipment",
            description = "Creates a new shipment with PENDING_VALIDATION status. The customer must present at the pickup location for validation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Shipment created successfully",
                    content = @Content(schema = @Schema(implementation = ShipmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not a customer", content = @Content)
    })
    public ResponseEntity<ShipmentResponse> createShipment(@Valid @RequestBody CustomerShipmentRequest request) {
        log.info("POST /customer/shipments - Creating shipment");

        var context = securityContextService.getCurrentSecurityContext();
        Shipment shipment = customerShipmentService.createShipment(context.userId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shipmentMapper.toResponseWithoutParcels(shipment));
    }

    /**
     * GET /customer/shipments
     * Liste tous les envois du client connecté.
     */
    @GetMapping
    @RequireActor(ActorType.CUSTOMER)
    @Operation(
            summary = "List customer shipments",
            description = "Lists all shipments created by the current customer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not a customer", content = @Content)
    })
    public ResponseEntity<List<ShipmentResponse>> listShipments(
            @RequestParam(required = false) String status) {
        log.info("GET /customer/shipments - status: {}", status);

        var context = securityContextService.getCurrentSecurityContext();

        List<Shipment> shipments;
        if (status != null && !status.isBlank()) {
            shipments = customerShipmentService.listCustomerShipmentsByStatus(context.userId(), status);
        } else {
            shipments = customerShipmentService.listCustomerShipments(context.userId());
        }

        return ResponseEntity.ok(shipmentMapper.toResponseList(shipments));
    }

    /**
     * GET /customer/shipments/{id}
     * Récupère un envoi par ID.
     */
    @GetMapping("/{id}")
    @RequireActor(ActorType.CUSTOMER)
    @Operation(
            summary = "Get shipment by ID",
            description = "Retrieves a specific shipment owned by the current customer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ShipmentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not a customer or not owner", content = @Content),
            @ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<ShipmentResponse> getShipment(@PathVariable UUID id) {
        log.info("GET /customer/shipments/{}", id);

        var context = securityContextService.getCurrentSecurityContext();
        Shipment shipment = customerShipmentService.getCustomerShipment(context.userId(), id);

        return ResponseEntity.ok(shipmentMapper.toResponse(shipment));
    }

    /**
     * PUT /customer/shipments/{id}
     * Met à jour un envoi en attente de validation.
     */
    @PutMapping("/{id}")
    @RequireActor(ActorType.CUSTOMER)
    @Operation(
            summary = "Update shipment",
            description = "Updates a shipment. Only PENDING_VALIDATION shipments can be modified by customers."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment updated successfully",
                    content = @Content(schema = @Schema(implementation = ShipmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or shipment not modifiable", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not a customer or not owner", content = @Content),
            @ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<ShipmentResponse> updateShipment(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerShipmentRequest request) {
        log.info("PUT /customer/shipments/{}", id);

        var context = securityContextService.getCurrentSecurityContext();
        Shipment shipment = customerShipmentService.updateShipment(context.userId(), id, request);

        return ResponseEntity.ok(shipmentMapper.toResponseWithoutParcels(shipment));
    }

    /**
     * DELETE /customer/shipments/{id}
     * Annule un envoi en attente de validation.
     */
    @DeleteMapping("/{id}")
    @RequireActor(ActorType.CUSTOMER)
    @Operation(
            summary = "Cancel shipment",
            description = "Cancels a shipment. Only PENDING_VALIDATION shipments can be cancelled by customers."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Shipment cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Shipment cannot be cancelled", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not a customer or not owner", content = @Content),
            @ApiResponse(responseCode = "404", description = "Shipment not found", content = @Content)
    })
    public ResponseEntity<Void> cancelShipment(@PathVariable UUID id) {
        log.info("DELETE /customer/shipments/{}", id);

        var context = securityContextService.getCurrentSecurityContext();
        customerShipmentService.cancelShipment(context.userId(), id);

        return ResponseEntity.noContent().build();
    }
}