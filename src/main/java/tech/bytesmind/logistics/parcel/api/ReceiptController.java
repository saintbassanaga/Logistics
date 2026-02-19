package tech.bytesmind.logistics.parcel.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import tech.bytesmind.logistics.parcel.application.service.ReceiptService;
import tech.bytesmind.logistics.parcel.application.service.ReceiptService.ReceiptType;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Documents", description = "PDF receipt and label generation")
public class ReceiptController {

    private final ReceiptService receiptService;

    /**
     * Full A4 shipment receipt. Only available when the shipment is CONFIRMED.
     * Embeds one QR code + Code-128 barcode for the shipment number.
     */
    @GetMapping(value = "/shipments/{id}/receipt", produces = "application/pdf")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Download shipment receipt PDF (CONFIRMED shipments only)")
    public ResponseEntity<byte[]> shipmentReceipt(@PathVariable UUID id) {
        byte[] pdf = receiptService.generateReceipt(id, ReceiptType.SHIPMENT);
        return pdfResponse(pdf, "receipt-" + id + ".pdf");
    }

    /**
     * A5 landscape parcel label. Available for any parcel status.
     * Embeds one QR code + Code-128 barcode for the parcel's tracking number.
     */
    @GetMapping(value = "/parcels/{id}/label", produces = "application/pdf")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Download individual parcel label PDF (A5 landscape)")
    public ResponseEntity<byte[]> parcelLabel(@PathVariable UUID id) {
        byte[] pdf = receiptService.generateReceipt(id, ReceiptType.PARCEL);
        return pdfResponse(pdf, "label-" + id + ".pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/pdf"))
                .body(pdf);
    }
}
