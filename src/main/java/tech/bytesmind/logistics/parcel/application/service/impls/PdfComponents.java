package tech.bytesmind.logistics.parcel.application.service.impls;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Font;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Reusable PDF building blocks: fonts, colors, QR/barcode generation,
 * and common cell/table constructors for OpenPDF.
 */
final class PdfComponents {

    // ── Colors ────────────────────────────────────────────────────────────────
    static final java.awt.Color HEADER_BG  = new java.awt.Color(26, 58, 92);   // #1a3a5c navy
    static final java.awt.Color SECTION_BG = new java.awt.Color(240, 244, 248);
    static final java.awt.Color ACCENT     = new java.awt.Color(41, 128, 185);  // #2980b9
    static final java.awt.Color ROW_ALT    = new java.awt.Color(248, 250, 252);
    static final java.awt.Color TEXT_DARK  = new java.awt.Color(30, 30, 30);
    static final java.awt.Color BORDER     = new java.awt.Color(210, 218, 226);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    static final Font TITLE;
    static final Font HEADER;
    static final Font SECTION_LABEL;
    static final Font LABEL;
    static final Font VALUE;
    static final Font VALUE_WHITE;  // VALUE-sized text on dark backgrounds
    static final Font SMALL;
    static final Font SMALL_WHITE;  // SMALL-sized text on dark backgrounds
    static final Font TRACKING_LARGE;
    static final Font STATUS_BADGE;

    static {
        try {
            BaseFont bf   = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.WINANSI, false);
            BaseFont bfB  = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
            TITLE         = new Font(bfB, 18, Font.NORMAL, java.awt.Color.WHITE);
            HEADER        = new Font(bfB, 11, Font.NORMAL, java.awt.Color.WHITE);
            SECTION_LABEL = new Font(bfB,  9, Font.NORMAL, ACCENT);
            LABEL         = new Font(bfB,  8, Font.NORMAL, TEXT_DARK);
            VALUE         = new Font(bf,   9, Font.NORMAL, TEXT_DARK);
            VALUE_WHITE   = new Font(bf,   9, Font.NORMAL, java.awt.Color.WHITE);
            SMALL         = new Font(bf,   7, Font.NORMAL, java.awt.Color.GRAY);
            SMALL_WHITE   = new Font(bf,   7, Font.NORMAL, java.awt.Color.WHITE);
            TRACKING_LARGE= new Font(bfB, 13, Font.NORMAL, TEXT_DARK);
            STATUS_BADGE  = new Font(bfB,  8, Font.NORMAL, java.awt.Color.WHITE);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to initialise PDF fonts: " + e.getMessage());
        }
    }

    private PdfComponents() {}

    // ── Image generation ──────────────────────────────────────────────────────

    static Image buildQrCode(String content, int sizePx) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx);
        return toImage(MatrixToImageWriter.toBufferedImage(matrix));
    }

    static Image buildBarcode(String content, int widthPx, int heightPx) throws Exception {
        Code128Writer writer = new Code128Writer();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.CODE_128, widthPx, heightPx);
        Image img = toImage(MatrixToImageWriter.toBufferedImage(matrix));
        img.setAlignment(Element.ALIGN_CENTER);
        return img;
    }

    private static Image toImage(BufferedImage buffered) throws IOException, BadElementException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buffered, "PNG", baos);
        return Image.getInstance(baos.toByteArray());
    }

    // ── Common cell helpers ───────────────────────────────────────────────────

    /** Cell with colored background, no border. */
    static PdfPCell coloredCell(Phrase content, java.awt.Color bg, int align, int padding) {
        PdfPCell cell = new PdfPCell(content);
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setPadding(padding);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    /** Standard bordered cell. */
    static PdfPCell borderedCell(Phrase content, int align, int padding) {
        PdfPCell cell = new PdfPCell(content);
        cell.setHorizontalAlignment(align);
        cell.setPadding(padding);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    static PdfPCell borderedCell(Phrase content, int align, int padding, java.awt.Color bg) {
        PdfPCell cell = borderedCell(content, align, padding);
        cell.setBackgroundColor(bg);
        return cell;
    }

    /** Section header spanning all columns (SECTION_BG + SECTION_LABEL font). */
    static PdfPCell sectionHeader(String text, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SECTION_LABEL));
        cell.setColspan(colspan);
        cell.setBackgroundColor(SECTION_BG);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthBottom(1f);
        cell.setBorderColorBottom(BORDER);
        return cell;
    }

    /** Dark (navy) header cell. */
    static PdfPCell darkCell(Phrase content, int align, int padding) {
        PdfPCell cell = new PdfPCell(content);
        cell.setBackgroundColor(HEADER_BG);
        cell.setHorizontalAlignment(align);
        cell.setPadding(padding);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    /** Convenience: single-line phrase. */
    static Phrase phrase(String text, Font font) {
        return new Phrase(text != null ? text : "", font);
    }

    /** Spacer paragraph. */
    static Paragraph spacer(float leading) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingBefore(leading);
        return p;
    }

    /** Full-width table (100%) with no spacing. */
    static PdfPTable fullWidthTable(int cols) {
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        table.setSpacingBefore(0);
        table.setSpacingAfter(0);
        return table;
    }

    /** Build a blank / padding cell (no content, no border). */
    static PdfPCell blankCell(int colspan, float height) {
        PdfPCell cell = new PdfPCell();
        cell.setColspan(colspan);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(height);
        return cell;
    }
}
