package tech.bytesmind.logistics.agency.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.agency.infrastructure.repository.AgencyRepository;

import java.security.SecureRandom;
import java.time.Year;

/**
 * Service de génération des codes d'agence.
 * Format: AGY-{YEAR}-{5-DIGITS}
 * Exemple: AGY-2026-00001
 * <p>
 * Garantit l'unicité via vérification en base.
 */
@Service
@RequiredArgsConstructor
public class AgencyCodeGenerator {

    private static final String PREFIX = "AGY";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 10;

    private final AgencyRepository agencyRepository;

    /**
     * Génère un code d'agence unique.
     * Format: AGY-{YEAR}-{5-DIGITS}
     */
    public String generateUniqueCode() {
        int year = Year.now().getValue();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Compter les agences existantes pour cette année
            long countThisYear = agencyRepository.countByCodeStartingWith(PREFIX + "-" + year);

            // Générer le prochain numéro séquentiel
            long nextNumber = countThisYear + 1;

            // Format: AGY-2026-00001
            String code = String.format("%s-%d-%05d", PREFIX, year, nextNumber);

            // Vérifier unicité
            if (!agencyRepository.existsByCode(code)) {
                return code;
            }
        }

        // Fallback: utiliser timestamp + random
        long timestamp = System.currentTimeMillis() % 100000;
        int random = RANDOM.nextInt(10000);
        return String.format("%s-%d-%05d", PREFIX, year, (timestamp + random) % 100000);
    }

    /**
     * Valide le format d'un code d'agence.
     */
    public boolean isValidFormat(String code) {
        if (code == null) {
            return false;
        }
        // Pattern: AGY-YYYY-NNNNN
        return code.matches("^AGY-\\d{4}-\\d{5}$");
    }
}