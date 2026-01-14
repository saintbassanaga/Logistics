package tech.bytesmind.logistics.parcel.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.parcel.infrastructure.repository.ParcelRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service class responsible for generating and validating unique tracking numbers.
 * Tracking numbers are generated following a specific format and include a checksum
 * for validation purposes.
 * <p>
 * The tracking number format is structured as follows:
 * TRK-{YYYYMMDD}-{RANDOM_PART}-{CHECKSUM}
 * - Prefix: A fixed prefix "TRK".
 * - Date (YYYYMMDD): The current date in year-month-day format.
 * - Random Part: An 8-character randomly generated string excluding confusing characters (e.g., 'I', 'O', '0', '1').
 * - Checksum: A single character derived from the previously mentioned parts using a checksum algorithm.
 */
@Service
@RequiredArgsConstructor
public class TrackingNumberGenerator {

    private static final String PREFIX = "TRK";
    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Sans I, O, 0, 1 (éviter confusion)
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_ATTEMPTS = 10;

    private final ParcelRepository parcelRepository;

    /**
     * Génère un numéro de tracking unique.
     * Format: TRK-{YYYYMMDD}-{8-CHARS}-{CHECKSUM}
     */
    public String generateUniqueTrackingNumber() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String trackingNumber = generateTrackingNumber();

            // Vérifier unicité
            if (!parcelRepository.existsByTrackingNumber(trackingNumber)) {
                return trackingNumber;
            }
        }

        // Si après 10 tentatives on n'a pas trouvé d'unique, on force avec timestamp
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(6);
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        String randomPart = generateRandomString(8);
        String base = dateStr + randomPart + timestamp;
        char checksum = calculateChecksum(base);

        return String.format("%s-%s-%s-%c", PREFIX, dateStr, randomPart, checksum);
    }

    /**
     * Génère un numéro de tracking.
     */
    private String generateTrackingNumber() {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        String randomPart = generateRandomString(8);
        String base = dateStr + randomPart;
        char checksum = calculateChecksum(base);

        return String.format("%s-%s-%s-%c", PREFIX, dateStr, randomPart, checksum);
    }

    /**
     * Génère une chaîne aléatoire de N caractères.
     */
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHARSET.length());
            sb.append(CHARSET.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Calcule le checksum en utilisant une variante de l'algorithme Luhn.
     * Retourne un caractère du CHARSET.
     */
    private char calculateChecksum(String input) {
        int sum = 0;
        boolean alternate = false;

        // Convertir chaque caractère en valeur numérique et calculer la somme
        for (int i = input.length() - 1; i >= 0; i--) {
            char c = input.charAt(i);
            int value;

            if (Character.isDigit(c)) {
                value = c - '0';
            } else {
                value = CHARSET.indexOf(c);
                if (value == -1) {
                    value = 0;
                }
            }

            if (alternate) {
                value *= 2;
                if (value > CHARSET.length()) {
                    value -= CHARSET.length();
                }
            }

            sum += value;
            alternate = !alternate;
        }

        // Le checksum est le complément à CHARSET.length()
        int checksumValue = (CHARSET.length() - (sum % CHARSET.length())) % CHARSET.length();
        return CHARSET.charAt(checksumValue);
    }

    /**
     * Valide un numéro de tracking (format + checksum).
     */
    public boolean isValid(String trackingNumber) {
        if (trackingNumber == null || !isValidFormat(trackingNumber)) {
            return false;
        }

        // Extraire les parties
        String[] parts = trackingNumber.split("-");
        if (parts.length != 4) {
            return false;
        }

        String dateStr = parts[1];
        String randomPart = parts[2];
        char providedChecksum = parts[3].charAt(0);

        // Recalculer le checksum
        String base = dateStr + randomPart;
        char calculatedChecksum = calculateChecksum(base);

        return providedChecksum == calculatedChecksum;
    }

    /**
     * Valide le format d'un numéro de tracking.
     */
    public boolean isValidFormat(String trackingNumber) {
        if (trackingNumber == null) {
            return false;
        }
        // Pattern: TRK-YYYYMMDD-XXXXXXXX-C
        return trackingNumber.matches("^TRK-\\d{8}-[A-Z0-9]{8}-[A-Z0-9]$");
    }
}
