package tech.bytesmind.logistics.shared.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class pour les tests d'intégration avec TestContainers.
 * <p>
 * - Lance un PostgreSQL container pour les tests
 * - Configure Spring Boot avec le contexte complet
 * - Fournit MockMvc pour tester les contrôleurs REST
 * - Rollback automatique après chaque test via @Transactional
 * <p>
 * Les tests héritant de cette classe ont accès à:
 * - Une vraie base de données PostgreSQL (via TestContainers)
 * - Tous les beans Spring (services, repositories, etc.)
 * - MockMvc pour envoyer des requêtes HTTP simulées
 * <p>
 * Usage:
 * <pre>
 * {@code
 * class MyControllerIntegrationTest extends BaseIntegrationTest {
 *     @Test
 *     void testEndpoint() throws Exception {
 *         mockMvc.perform(get("/api/endpoint"))
 *                .andExpect(status().isOk());
 *     }
 * }
 * }
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    /**
     * Container PostgreSQL partagé entre tous les tests.
     * TestContainers réutilise le même container pour tous les tests
     * de la suite pour améliorer les performances.
     */
    @Container
    @ServiceConnection
    protected static PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("logistics_test")
                    .withUsername("test_user")
                    .withPassword("test_password")
                    .withReuse(true);

    /**
     * MockMvc pour envoyer des requêtes HTTP simulées aux contrôleurs.
     * Permet de tester les endpoints REST sans démarrer un vrai serveur HTTP.
     */
    @Autowired
    protected MockMvc mockMvc;

    /**
     * Setup exécuté avant chaque test.
     * Peut être surchargé dans les classes filles pour ajouter du setup spécifique.
     */
    @BeforeEach
    protected void setUp() {
        // Hook pour les classes filles
    }

    /**
     * Helper: Crée un JWT mocké pour les tests d'authentification.
     * À surcharger dans les classes filles si nécessaire.
     */
    protected String createMockJwt(String userId, String actorType, String agencyId) {
        // TODO: Implémenter la création de JWT mocké pour les tests
        // Utiliser MockMvc avec @WithMockUser ou SecurityMockMvcRequestPostProcessors
        return "mock-jwt-token";
    }
}
