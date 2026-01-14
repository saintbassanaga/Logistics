package tech.bytesmind.logistics.parcel.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.parcel.infrastructure.repository.ShipmentRepository;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service responsable de la génération et de la validation des numéros uniques pour les shipments.
 * Fournit une méthode pour générer des identifiants de shipment uniques basés sur une combinaison de
 * la date, un préfixe d'agence dérivé de son UUID, et un numéro séquentiel ou aléatoire.
 */
@Service
@RequiredArgsConstructor
public class ShipmentNumberGenerator {

    private static final String PREFIX = "SHP";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_ATTEMPTS = 10;

    private final ShipmentRepository shipmentRepository;

    /**
     * Génère un numéro de shipment unique pour une agence.
     * Format: SHP-{YYYYMMDD}-{AGENCY_PREFIX}-{6-DIGITS}
     *
     * @param agencyId UUID de l'agence (utilisé pour générer le préfixe)
     */
    public String generateUniqueNumber(UUID agencyId) {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        String agencyPrefix = generateAgencyPrefix(agencyId);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Compter les shipments pour cette agence aujourd'hui
            long countToday = shipmentRepository.countByAgencyIdAndShipmentNumberStartingWith(
                    agencyId,
                    PREFIX + "-" + dateStr + "-" + agencyPrefix
            );

            // Générer le prochain numéro séquentiel
            long nextNumber = countToday + 1;

            // Format: SHP-20260114-A5F-000001
            String number = String.format("%s-%s-%s-%06d", PREFIX, dateStr, agencyPrefix, nextNumber);

            // Vérifier unicité
            if (!shipmentRepository.existsByShipmentNumber(number)) {
                return number;
            }
        }

        // Fallback: utiliser random
        int random = RANDOM.nextInt(1000000);
        return String.format("%s-%s-%s-%06d", PREFIX, dateStr, agencyPrefix, random);
    }

    /**
     * Génère un préfixe de 3 caractères à partir de l'UUID de l'agence.
     * Utilise les 3 premiers caractères hexadécimaux en majuscule.
     */
    private String generateAgencyPrefix(UUID agencyId) {
        return agencyId.toString()
                .replace("-", "")
                .substring(0, 3)
                .toUpperCase();
    }

    /**
     * Valide le format d'un numéro de shipment.
     */
    public boolean isValidFormat(String number) {
        if (number == null) {
            return false;
        }
        // Pattern: SHP-YYYYMMDD-XXX-NNNNNN
        return number.matches("^SHP-\\d{8}-[A-Z0-9]{3}-\\d{6}$");
    }
}
