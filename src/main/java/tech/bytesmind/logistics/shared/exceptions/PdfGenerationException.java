package tech.bytesmind.logistics.shared.exceptions;

/**
 * Thrown when PDF rendering fails due to a library or I/O error
 * (ZXing encoding, OpenPDF document writing, etc.).
 *
 * Distinct from {@link BusinessException} so the global handler can
 * return HTTP 500 instead of 400/409.
 */
public class PdfGenerationException extends RuntimeException {

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
