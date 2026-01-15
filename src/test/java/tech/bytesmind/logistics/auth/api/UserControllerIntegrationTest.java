package tech.bytesmind.logistics.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import tech.bytesmind.logistics.auth.api.dto.CreateUserRequest;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.domain.model.RoleScope;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.auth.infrastructure.repository.RoleRepository;
import tech.bytesmind.logistics.auth.infrastructure.repository.UserRepository;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.test.BaseIntegrationTest;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour UserController.
 * <p>
 * Ces tests vérifient:
 * - L'intégration complète entre contrôleur, service, repository, et base de données
 * - La sécurité RBAC/ABAC
 * - La validation des DTOs
 * - Le mapping JSON
 * - Les codes de statut HTTP
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("UserController Integration Tests")
@ActiveProfiles("test")
class UserControllerIntegrationTest extends BaseIntegrationTest {


    @Autowired
    private UserRepository userRepository;


    @Autowired
    private  RoleRepository roleRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();


    private UUID testAgencyId;

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();

        // Créer un rôle de test
        Role agencyAdminRole = new Role();
        agencyAdminRole.setCode("AGENCY_ADMIN");
        agencyAdminRole.setName("Agency Administrator");
        agencyAdminRole.setScope(RoleScope.AGENCY);
        agencyAdminRole.setActive(true);
        roleRepository.save(agencyAdminRole);

        testAgencyId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /users - Should create user as PLATFORM_ADMIN")
    void createUser_AsPlatformAdmin_ShouldSucceed() throws Exception {
        // Given: Un PLATFORM_ADMIN authentifié
        CreateUserRequest request = new CreateUserRequest(
                "test@example.com",
                "John",
                "Doe",
                "+33612345678",
                ActorType.AGENCY_EMPLOYEE,
                testAgencyId,
                "Manager",
                "Operations", null
        );

        // When: POST /users
        ResultActions result = mockMvc.perform(post("/users")
                .with(jwt().jwt(jwt -> jwt
                        .claim("sub", UUID.randomUUID().toString())
                        .claim("actor_type", "PLATFORM_ADMIN")
                        .claim("roles", new String[]{"PLATFORM_ADMIN"})
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Then: 201 Created avec UserResponse
        result.andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.actorType").value("AGENCY_EMPLOYEE"))
                .andExpect(jsonPath("$.agencyId").value(testAgencyId.toString()))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("POST /users - Should fail when AGENCY_EMPLOYEE without agencyId")
    void createUser_AgencyEmployeeWithoutAgencyId_ShouldFail() throws Exception {
        // Given: Request AGENCY_EMPLOYEE sans agencyId (violation contrainte métier)
        CreateUserRequest request = new CreateUserRequest(
                "test@example.com",
                "John",
                "Doe",
                null,
                ActorType.AGENCY_EMPLOYEE,
                null,
                null,
                null, null
        );

        // When: POST /users
        ResultActions result = mockMvc.perform(post("/users")
                .with(jwt().jwt(jwt -> jwt
                        .claim("sub", UUID.randomUUID().toString())
                        .claim("actor_type", "PLATFORM_ADMIN")
                        .claim("roles", new String[]{"PLATFORM_ADMIN"})
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Then: 400 Bad Request (validation métier échoue)
        result.andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /users/{id} - Should retrieve user when authorized")
    void getUser_WhenAuthorized_ShouldSucceed() throws Exception {
        // Given: Un utilisateur existant
        User user = new User();
        user.setEmail("existing@example.com");
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setActorType(ActorType.AGENCY_EMPLOYEE);
        user.setAgencyId(testAgencyId);
        user.setActive(true);
        user = userRepository.save(user);

        // When: GET /users/{id} par un PLATFORM_ADMIN
        ResultActions result = mockMvc.perform(get("/users/{id}", user.getId())
                .with(jwt().jwt(jwt -> jwt
                        .claim("sub", UUID.randomUUID().toString())
                        .claim("actor_type", "PLATFORM_ADMIN")
                        .claim("roles", new String[]{"PLATFORM_ADMIN"})
                )));

        // Then: 200 OK avec UserResponse
        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("existing@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    @DisplayName("GET /users/{id} - Should deny access when not authorized")
    void getUser_WhenNotAuthorized_ShouldDenyAccess() throws Exception {
        UUID agencyA = UUID.randomUUID();
        User userA = new User();
        userA.setEmail("user-a@example.com");
        userA.setFirstName("User");
        userA.setLastName("A");
        userA.setActorType(ActorType.AGENCY_EMPLOYEE);
        userA.setAgencyId(agencyA);
        userA.setActive(true);
        userA = userRepository.save(userA);

        UUID agencyB = UUID.randomUUID();
        UUID employeeBId = UUID.randomUUID();
        ResultActions result = mockMvc.perform(get("/users/{id}", userA.getId())
                .with(jwt().jwt(jwt -> jwt
                        .claim("sub", employeeBId.toString())
                        .claim("actor_type", "AGENCY_EMPLOYEE")
                        .claim("agency_id", agencyB.toString())
                        .claim("roles", new String[]{"AGENCY_ADMIN"})
                )));

        // Then: 403 Forbidden (ABAC: pas de la même agence)
        result.andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users/me - Should return current user profile")
    void getCurrentUser_ShouldReturnOwnProfile() throws Exception {
        // Given: Un utilisateur authentifié
        User currentUser = new User();
        currentUser.setEmail("current@example.com");
        currentUser.setFirstName("Current");
        currentUser.setLastName("User");
        currentUser.setActorType(ActorType.AGENCY_EMPLOYEE);
        currentUser.setAgencyId(testAgencyId);
        currentUser.setActive(true);
        currentUser = userRepository.save(currentUser);

        // When: GET /users/me
        User finalCurrentUser = currentUser;
        ResultActions result = mockMvc.perform(get("/users/me")
                .with(jwt().jwt(jwt -> jwt
                        .claim("sub", finalCurrentUser.getId().toString())
                        .claim("actor_type", "AGENCY_EMPLOYEE")
                        .claim("agency_id", testAgencyId.toString())
                        .claim("roles", new String[]{})
                )));

        // Then: 200 OK avec son propre profil
        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(currentUser.getId().toString()))
                .andExpect(jsonPath("$.email").value("current@example.com"));
    }

    @Test
    @DisplayName("POST /users - Should fail without authentication")
    void createUser_WithoutAuth_ShouldFail() throws Exception {
        // Given: Une requête sans JWT
        CreateUserRequest request = new CreateUserRequest(
                "test@example.com",
                "John",
                "Doe",
                null,
                ActorType.CUSTOMER,
                null,
                null,
                null, null
        );

        // When: POST /users sans authentification
        ResultActions result = mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Then: 401 Unauthorized
        result.andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
