package tech.bytesmind.logistics.parcel.application.service;

import java.util.UUID;

/**
 * Single entry-point for PDF document generation.
 *
 * <p>One generic method dispatches to the appropriate document layout based on
 * the {@link ReceiptType}:</p>
 * <ul>
 *   <li>{@link ReceiptType#SHIPMENT} — A4 full shipment receipt (requires {@code CONFIRMED} status),
 *       one QR code for the shipment number, one Code-128 barcode, full parcel table.</li>
 *   <li>{@link ReceiptType#PARCEL} — A5 landscape parcel label, one QR code per parcel tracking
 *       number, one Code-128 barcode, sender/receiver block.</li>
 * </ul>
 */
public interface ReceiptService {

    /**
     * Generate a PDF receipt or label for the given entity.
     *
     * @param entityId the UUID of the {@code Shipment} (for {@link ReceiptType#SHIPMENT})
     *                 or of the {@code Parcel} (for {@link ReceiptType#PARCEL})
     * @param type     which document to generate
     * @return raw PDF bytes ready for streaming
     * @throws tech.bytesmind.logistics.shared.exceptions.BusinessException
     *         if the entity is not found, or if a shipment receipt is requested for a
     *         non-CONFIRMED shipment
     */
    byte[] generateReceipt(UUID entityId, ReceiptType type);

    /** Identifies which PDF document to produce. */
    enum ReceiptType {
        /** Full A4 portrait shipment receipt — only available when shipment status is CONFIRMED. */
        SHIPMENT,
        /** A5 landscape parcel label — available for any parcel status. */
        PARCEL
    }
}
