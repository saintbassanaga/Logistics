package tech.bytesmind.logistics.parcel.application.service.impls;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.agency.domain.model.Agency;
import tech.bytesmind.logistics.agency.domain.model.AgencyLocation;
import tech.bytesmind.logistics.agency.infrastructure.repository.AgencyLocationRepository;
import tech.bytesmind.logistics.agency.infrastructure.repository.AgencyRepository;
import tech.bytesmind.logistics.parcel.application.policy.ParcelAccessPolicy;
import tech.bytesmind.logistics.parcel.application.policy.ShipmentAccessPolicy;
import tech.bytesmind.logistics.parcel.application.service.ReceiptService;
import tech.bytesmind.logistics.parcel.domain.model.Parcel;
import tech.bytesmind.logistics.parcel.domain.model.Shipment;
import tech.bytesmind.logistics.parcel.domain.model.ShipmentStatus;
import tech.bytesmind.logistics.parcel.infrastructure.repository.ParcelRepository;
import tech.bytesmind.logistics.parcel.infrastructure.repository.ShipmentRepository;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;
import tech.bytesmind.logistics.shared.exceptions.PdfGenerationException;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.model.SecurityContext;
import tech.bytesmind.logistics.shared.security.service.SecurityContextService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static tech.bytesmind.logistics.parcel.application.service.impls.PdfComponents.*;

/**
 * PDF generation service.
 *
 * <p>A single {@link #generateReceipt} method dispatches to the correct rendering
 * pipeline based on {@link ReceiptType}. Both pipelines enforce:</p>
 * <ol>
 *   <li>ABAC access policy (ShipmentAccessPolicy / ParcelAccessPolicy)</li>
 *   <li>Multi-tenant isolation — AGENCY_EMPLOYEE queries are filtered by agencyId
 *       at the repository level, hiding the existence of other tenants' data</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptServiceImpl implements ReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptServiceImpl.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TS_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'", Locale.ENGLISH);

    // ── Repositories ──────────────────────────────────────────────────────────
    private final ShipmentRepository       shipmentRepository;
    private final ParcelRepository         parcelRepository;
    private final AgencyRepository         agencyRepository;
    private final AgencyLocationRepository agencyLocationRepository;

    // ── Security ──────────────────────────────────────────────────────────────
    private final SecurityContextService  securityContextService;
    private final ShipmentAccessPolicy    shipmentAccessPolicy;
    private final ParcelAccessPolicy      parcelAccessPolicy;

    // ─────────────────────────────────────────────────────────────────────────
    // Generic entry point — dispatches by ReceiptType
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public byte[] generateReceipt(UUID entityId, ReceiptType type) {
        return switch (type) {
            case SHIPMENT -> renderShipmentReceipt(entityId);
            case PARCEL   -> renderParcelLabel(entityId);
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHIPMENT — A4 portrait full receipt
    // ─────────────────────────────────────────────────────────────────────────

    private byte[] renderShipmentReceipt(UUID shipmentId) {
        log.debug("Rendering shipment receipt for {}", shipmentId);

        SecurityContext ctx = securityContextService.getCurrentSecurityContext();

        // Multi-tenant query: AGENCY_EMPLOYEE sees only their own agency's shipments
        Shipment shipment = (ctx.actorType() == ActorType.PLATFORM_ADMIN)
                ? shipmentRepository.findByIdWithParcels(shipmentId)
                    .orElseThrow(() -> new BusinessException("Shipment not found: " + shipmentId))
                : shipmentRepository.findByIdAndAgencyIdWithParcels(shipmentId, ctx.agencyId())
                    .orElseThrow(() -> new BusinessException("Shipment not found: " + shipmentId));

        // ABAC — documents intent and covers edge cases (e.g. PLATFORM_ADMIN with restrictions)
        shipmentAccessPolicy.validateAccess(ctx, shipment.getAgencyId());

        // Business rule: receipt only for CONFIRMED shipments
        if (shipment.getStatus() != ShipmentStatus.CONFIRMED) {
            throw new BusinessException(
                "Receipt is only available for CONFIRMED shipments. Current status: " + shipment.getStatus()
            );
        }

        Agency agency = agencyRepository.findById(shipment.getAgencyId())
                .orElseThrow(() -> new BusinessException("Agency not found: " + shipment.getAgencyId()));

        AgencyLocation pickup = Optional.ofNullable(shipment.getPickupLocationId())
                .flatMap(agencyLocationRepository::findById)
                .orElse(null);

        List<Parcel> parcels = new ArrayList<>(shipment.getParcels());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            doc.add(buildAgencyHeader(agency, shipment));
            doc.add(spacer(6));

            if (pickup != null) {
                doc.add(buildPickupSection(pickup));
                doc.add(spacer(6));
            }

            doc.add(buildSenderReceiver(shipment));
            doc.add(spacer(8));

            doc.add(buildParcelsTable(parcels));
            doc.add(spacer(4));
            doc.add(buildSummary(shipment, parcels));

            if (shipment.getNotes() != null && !shipment.getNotes().isBlank()) {
                doc.add(spacer(6));
                doc.add(buildNotes(shipment.getNotes()));
            }

            doc.add(spacer(10));
            doc.add(buildShipmentFooter(shipment));

            doc.close();
            log.info("Shipment receipt generated for {} ({} bytes)", shipmentId, baos.size());
            return baos.toByteArray();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // PDF library / I/O failure — not a business rule violation → 500
            throw new PdfGenerationException("Failed to render shipment receipt: " + shipmentId, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARCEL — A6 portrait label
    // ─────────────────────────────────────────────────────────────────────────

    private byte[] renderParcelLabel(UUID parcelId) {
        log.debug("Rendering parcel label for {}", parcelId);

        SecurityContext ctx = securityContextService.getCurrentSecurityContext();

        // Multi-tenant query: AGENCY_EMPLOYEE sees only their own agency's parcels
        Parcel parcel = (ctx.actorType() == ActorType.PLATFORM_ADMIN)
                ? parcelRepository.findByIdWithShipment(parcelId)
                    .orElseThrow(() -> new BusinessException("Parcel not found: " + parcelId))
                : parcelRepository.findByIdAndAgencyIdWithShipment(parcelId, ctx.agencyId())
                    .orElseThrow(() -> new BusinessException("Parcel not found: " + parcelId));

        // ABAC
        parcelAccessPolicy.validateAccess(ctx, parcel.getAgencyId());

        Shipment shipment = parcel.getShipment();

        Agency agency = agencyRepository.findById(parcel.getAgencyId())
                .orElseThrow(() -> new BusinessException("Agency not found: " + parcel.getAgencyId()));

        AgencyLocation pickup = Optional.ofNullable(shipment.getPickupLocationId())
                .flatMap(agencyLocationRepository::findById)
                .orElse(null);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A6, 12, 12, 12, 12);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            doc.add(buildLabelHeader(agency));
            doc.add(spacer(4));
            doc.add(buildLabelFromTo(parcel, shipment));
            doc.add(spacer(4));
            doc.add(buildLabelTracking(parcel));
            doc.add(spacer(4));
            doc.add(buildLabelBarcode(parcel.getTrackingNumber()));
            doc.add(spacer(4));
            doc.add(buildLabelFooter(parcel, shipment, pickup));

            doc.close();
            log.info("Parcel label generated for {} ({} bytes)", parcelId, baos.size());
            return baos.toByteArray();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to render parcel label: " + parcelId, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shipment receipt sections
    // ─────────────────────────────────────────────────────────────────────────

    private PdfPTable buildAgencyHeader(Agency agency, Shipment shipment) {
        PdfPTable table = fullWidthTable(2);

        Paragraph left = new Paragraph();
        left.add(new Chunk(agency.getName() + "\n", TITLE));
        left.add(new Chunk(formatAddress(agency.getAddressLine1(), agency.getCity(),
                agency.getPostalCode(), agency.getCountry()) + "\n", VALUE_WHITE));
        if (agency.getPhone() != null) left.add(new Chunk("\u260E " + agency.getPhone() + "\n", VALUE_WHITE));
        if (agency.getEmail() != null) left.add(new Chunk(agency.getEmail() + "\n", SMALL_WHITE));
        if (agency.getTaxId() != null) left.add(new Chunk("Tax ID: " + agency.getTaxId(), SMALL_WHITE));

        Paragraph right = new Paragraph();
        right.setAlignment(Element.ALIGN_RIGHT);
        right.add(new Chunk("SHIPMENT RECEIPT\n", HEADER));
        right.add(new Chunk(shipment.getShipmentNumber() + "\n", VALUE_WHITE));
        right.add(new Chunk("\u25CF " + shipment.getStatus().name() + "\n", VALUE_WHITE));
        if (shipment.getConfirmedAt() != null) {
            String date = ZonedDateTime.ofInstant(shipment.getConfirmedAt(), ZoneOffset.UTC).format(DATE_FMT);
            right.add(new Chunk(date, SMALL_WHITE));
        }

        table.addCell(compositeCell(left,  HEADER_BG, Element.ALIGN_LEFT,  14));
        table.addCell(compositeCell(right, HEADER_BG, Element.ALIGN_RIGHT, 14));
        return table;
    }

    private PdfPTable buildPickupSection(AgencyLocation loc) throws DocumentException {
        PdfPTable table = fullWidthTable(1);
        table.addCell(sectionHeader("PICKUP LOCATION", 1));

        Paragraph p = new Paragraph();
        p.add(new Chunk(loc.getName(), LABEL));
        p.add(new Chunk("  [" + loc.getLocationType().name() + "]", SMALL));
        p.add(Chunk.NEWLINE);
        p.add(new Chunk(formatAddress(loc.getAddressLine1(), loc.getCity(), loc.getPostalCode(), loc.getCountry()), VALUE));
        if (loc.getPhone() != null) { p.add(Chunk.NEWLINE); p.add(new Chunk("\u260E " + loc.getPhone(), VALUE)); }
        if (loc.getContactPersonName() != null) {
            p.add(Chunk.NEWLINE);
            p.add(new Chunk("Contact: " + loc.getContactPersonName(), VALUE));
            if (loc.getContactPersonPhone() != null) p.add(new Chunk(" · " + loc.getContactPersonPhone(), VALUE));
        }

        PdfPCell cell = new PdfPCell();
        cell.addElement(p);
        cell.setPadding(8);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
        return table;
    }

    private PdfPTable buildSenderReceiver(Shipment s) throws DocumentException {
        PdfPTable table = fullWidthTable(2);
        table.addCell(sectionHeader("FROM", 1));
        table.addCell(sectionHeader("TO", 1));
        table.addCell(addressCell(s.getSenderName(), s.getSenderPhone(), s.getSenderEmail(),
                s.getSenderAddressLine1(), s.getSenderAddressLine2(),
                s.getSenderCity(), s.getSenderPostalCode(), s.getSenderCountry()));
        table.addCell(addressCell(s.getReceiverName(), s.getReceiverPhone(), s.getReceiverEmail(),
                s.getReceiverAddressLine1(), s.getReceiverAddressLine2(),
                s.getReceiverCity(), s.getReceiverPostalCode(), s.getReceiverCountry()));
        return table;
    }

    private PdfPTable buildParcelsTable(List<Parcel> parcels) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{0.35f, 1.5f, 0.65f, 1.0f, 0.85f, 0.75f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        for (String h : new String[]{"#", "Tracking / Description", "Weight", "Dimensions", "Value", "Status"}) {
            PdfPCell hCell = new PdfPCell(phrase(h, SECTION_LABEL));
            hCell.setBackgroundColor(SECTION_BG);
            hCell.setPadding(5);
            hCell.setBorderColor(BORDER);
            hCell.setBorderWidth(0.5f);
            table.addCell(hCell);
        }

        for (int i = 0; i < parcels.size(); i++) {
            Parcel p     = parcels.get(i);
            java.awt.Color rowBg = (i % 2 == 0) ? java.awt.Color.WHITE : ROW_ALT;

            table.addCell(borderedCell(phrase(String.valueOf(i + 1), VALUE), Element.ALIGN_CENTER, 6, rowBg));

            Paragraph trackP = new Paragraph();
            trackP.add(new Chunk(p.getTrackingNumber() + "\n", LABEL));
            if (p.getDescription() != null && !p.getDescription().isBlank())
                trackP.add(new Chunk(p.getDescription(), SMALL));
            PdfPCell trackCell = new PdfPCell();
            trackCell.addElement(trackP);
            trackCell.setPadding(6);
            trackCell.setBorderColor(BORDER);
            trackCell.setBorderWidth(0.5f);
            trackCell.setBackgroundColor(rowBg);
            table.addCell(trackCell);

            table.addCell(borderedCell(phrase(p.getWeight() != null ? p.getWeight().toPlainString() + " kg" : "-", VALUE), Element.ALIGN_CENTER, 6, rowBg));
            table.addCell(borderedCell(phrase(formatDimensions(p), VALUE), Element.ALIGN_CENTER, 6, rowBg));
            table.addCell(borderedCell(phrase(p.getDeclaredValue() != null ? p.getDeclaredValue().toPlainString() + " " + orEmpty(p.getCurrency()) : "-", VALUE), Element.ALIGN_CENTER, 6, rowBg));
            table.addCell(borderedCell(phrase(p.getStatus().name(), SMALL), Element.ALIGN_CENTER, 6, rowBg));
        }
        return table;
    }

    private Paragraph buildSummary(Shipment shipment, List<Parcel> parcels) {
        BigDecimal totalWeight = parcels.stream().map(Parcel::getWeight).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalValue  = parcels.stream().map(Parcel::getDeclaredValue).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        String text = String.format("Total: %d parcel%s  ·  %s kg  ·  %s %s",
                parcels.size(), parcels.size() != 1 ? "s" : "",
                totalWeight.stripTrailingZeros().toPlainString(),
                totalValue.toPlainString(),
                orEmpty(shipment.getCurrency()));
        Paragraph p = new Paragraph(text, VALUE);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private PdfPTable buildNotes(String notes) throws DocumentException {
        PdfPTable table = fullWidthTable(1);
        table.addCell(sectionHeader("NOTES", 1));
        PdfPCell cell = new PdfPCell(phrase(notes, VALUE));
        cell.setPadding(8);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
        return table;
    }

    /**
     * Footer: one QR code for the shipment + Code-128 barcode + print timestamp.
     * One QR per shipment — not per parcel.
     */
    private PdfPTable buildShipmentFooter(Shipment shipment) throws Exception {
        PdfPTable table = fullWidthTable(1);

        Image qr = buildQrCode(shipment.getShipmentNumber(), 90);
        qr.setAlignment(Element.ALIGN_CENTER);
        PdfPCell qrCell = new PdfPCell(qr, false);
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setPadding(4);
        table.addCell(qrCell);

        Image barcode = buildBarcode(shipment.getShipmentNumber(), 400, 50);
        PdfPCell bcCell = new PdfPCell(barcode, true);
        bcCell.setBorder(Rectangle.NO_BORDER);
        bcCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        bcCell.setPadding(2);
        table.addCell(bcCell);

        Paragraph ts = new Paragraph("Printed: " + ZonedDateTime.now(ZoneOffset.UTC).format(TS_FMT), SMALL);
        ts.setAlignment(Element.ALIGN_CENTER);
        PdfPCell tsCell = new PdfPCell();
        tsCell.addElement(ts);
        tsCell.setBorder(Rectangle.NO_BORDER);
        tsCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tsCell.setPaddingTop(4);
        table.addCell(tsCell);

        return table;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parcel label sections
    // ─────────────────────────────────────────────────────────────────────────

    private PdfPTable buildLabelHeader(Agency agency) {
        PdfPTable table = fullWidthTable(2);
        Paragraph name  = new Paragraph(agency.getName(), TITLE);
        Paragraph title = new Paragraph("PARCEL LABEL", HEADER);
        title.setAlignment(Element.ALIGN_RIGHT);
        table.addCell(compositeCell(name,  HEADER_BG, Element.ALIGN_LEFT,  10));
        table.addCell(compositeCell(title, HEADER_BG, Element.ALIGN_RIGHT, 10));
        return table;
    }

    private PdfPTable buildLabelFromTo(Parcel parcel, Shipment shipment) throws DocumentException {
        PdfPTable table = fullWidthTable(2);
        table.addCell(sectionHeader("FROM", 1));
        table.addCell(sectionHeader("TO",   1));

        table.addCell(addressCell(shipment.getSenderName(), shipment.getSenderPhone(), null,
                shipment.getSenderAddressLine1(), shipment.getSenderAddressLine2(),
                shipment.getSenderCity(), shipment.getSenderPostalCode(), shipment.getSenderCountry()));

        // Parcel-level receiver overrides take priority over shipment-level
        String rcvName    = parcel.getSpecificReceiverName()    != null ? parcel.getSpecificReceiverName()    : shipment.getReceiverName();
        String rcvPhone   = parcel.getSpecificReceiverPhone()   != null ? parcel.getSpecificReceiverPhone()   : shipment.getReceiverPhone();
        String rcvAddress = parcel.getSpecificReceiverAddress();

        if (rcvAddress != null) {
            // Override: single free-text address line
            PdfPCell cell = new PdfPCell();
            Paragraph p = new Paragraph();
            p.add(new Chunk(orEmpty(rcvName) + "\n", LABEL));
            p.add(new Chunk(rcvAddress + "\n", VALUE));
            if (rcvPhone != null) p.add(new Chunk("\u260E " + rcvPhone, VALUE));
            cell.addElement(p);
            cell.setPadding(8);
            cell.setBorderColor(BORDER);
            cell.setBorderWidth(0.5f);
            table.addCell(cell);
        } else {
            table.addCell(addressCell(rcvName, rcvPhone, shipment.getReceiverEmail(),
                    shipment.getReceiverAddressLine1(), shipment.getReceiverAddressLine2(),
                    shipment.getReceiverCity(), shipment.getReceiverPostalCode(), shipment.getReceiverCountry()));
        }
        return table;
    }

    private PdfPTable buildLabelTracking(Parcel parcel) throws Exception {
        PdfPTable table = fullWidthTable(2);
        table.setWidths(new float[]{0.6f, 1.4f});

        Image qr = buildQrCode(parcel.getTrackingNumber(), 90);
        qr.setAlignment(Element.ALIGN_CENTER);
        PdfPCell qrCell = new PdfPCell(qr, false);
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        qrCell.setPadding(4);
        table.addCell(qrCell);

        Paragraph details = new Paragraph();
        details.add(new Chunk(parcel.getTrackingNumber() + "\n", TRACKING_LARGE));
        if (parcel.getWeight() != null)
            details.add(new Chunk("Weight: " + parcel.getWeight().toPlainString() + " kg\n", VALUE));
        details.add(new Chunk("Dimensions: " + formatDimensions(parcel) + "\n", VALUE));
        if (parcel.getDescription() != null && !parcel.getDescription().isBlank())
            details.add(new Chunk("Content: " + parcel.getDescription() + "\n", VALUE));
        if (parcel.getDeclaredValue() != null)
            details.add(new Chunk("Declared Value: " + parcel.getDeclaredValue().toPlainString()
                    + " " + orEmpty(parcel.getCurrency()), VALUE));

        PdfPCell detailCell = new PdfPCell();
        detailCell.addElement(details);
        detailCell.setBorder(Rectangle.NO_BORDER);
        detailCell.setPadding(6);
        detailCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(detailCell);
        return table;
    }

    private PdfPTable buildLabelBarcode(String trackingNumber) throws Exception {
        PdfPTable table = fullWidthTable(1);

        Image barcode = buildBarcode(trackingNumber, 270, 40);
        PdfPCell bc = new PdfPCell(barcode, true);
        bc.setBorderColor(BORDER);
        bc.setBorderWidth(0.5f);
        bc.setHorizontalAlignment(Element.ALIGN_CENTER);
        bc.setPadding(4);
        table.addCell(bc);

        PdfPCell text = new PdfPCell(phrase(trackingNumber, SMALL));
        text.setHorizontalAlignment(Element.ALIGN_CENTER);
        text.setBorder(Rectangle.NO_BORDER);
        text.setPaddingBottom(2);
        table.addCell(text);

        return table;
    }

    private PdfPTable buildLabelFooter(Parcel parcel, Shipment shipment, AgencyLocation pickup) {
        PdfPTable table = fullWidthTable(1);

        Paragraph p = new Paragraph();
        p.add(new Chunk("Shipment: " + shipment.getShipmentNumber() + "   ", SMALL));
        p.add(new Chunk("Status: " + parcel.getStatus().name() + "   ", SMALL));
        if (shipment.getCreatedAt() != null) {
            String created = ZonedDateTime.ofInstant(shipment.getCreatedAt(), ZoneOffset.UTC).format(DATE_FMT);
            p.add(new Chunk("Created: " + created + "   ", SMALL));
        }
        if (pickup != null) p.add(new Chunk("Pickup: " + pickup.getName(), SMALL));

        PdfPCell cell = new PdfPCell();
        cell.addElement(p);
        cell.setBackgroundColor(SECTION_BG);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthTop(0.5f);
        cell.setBorderColorTop(BORDER);
        table.addCell(cell);
        return table;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────────

    private PdfPCell addressCell(String name, String phone, String email,
                                  String line1, String line2,
                                  String city, String postal, String country) {
        PdfPCell cell = new PdfPCell();
        Paragraph p = new Paragraph();
        p.add(new Chunk(orEmpty(name) + "\n", LABEL));
        if (line1 != null) p.add(new Chunk(line1 + "\n", VALUE));
        if (line2 != null && !line2.isBlank()) p.add(new Chunk(line2 + "\n", VALUE));
        p.add(new Chunk(orEmpty(city) + " " + orEmpty(postal) + " · " + orEmpty(country) + "\n", VALUE));
        if (phone != null) p.add(new Chunk("\u260E " + phone + "\n", VALUE));
        if (email != null) p.add(new Chunk(email, SMALL));
        cell.addElement(p);
        cell.setPadding(8);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    /** Wraps a Paragraph into a dark/colored PdfPCell. */
    private PdfPCell compositeCell(Paragraph content, java.awt.Color bg, int align, int padding) {
        content.setAlignment(align);
        PdfPCell cell = new PdfPCell();
        cell.addElement(content);
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setPadding(padding);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private String formatAddress(String line1, String city, String postal, String country) {
        return orEmpty(line1) + ", " + orEmpty(city) + " " + orEmpty(postal) + ", " + orEmpty(country);
    }

    private String formatDimensions(Parcel p) {
        if (p.getLength() == null && p.getWidth() == null && p.getHeight() == null) return "-";
        return orDash(p.getLength()) + " × " + orDash(p.getWidth()) + " × " + orDash(p.getHeight()) + " cm";
    }

    private String orDash(BigDecimal v)  { return v != null ? v.toPlainString() : "-"; }
    private String orEmpty(String s)     { return s != null ? s : ""; }
}
