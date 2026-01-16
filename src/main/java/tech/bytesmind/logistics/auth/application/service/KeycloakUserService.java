package tech.bytesmind.logistics.auth.application.service;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.auth.api.dto.KeycloakCreateUserRequest;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.auth.infrastructure.repository.UserRepository;
import tech.bytesmind.logistics.shared.exceptions.KeycloakAdminException;
import tech.bytesmind.logistics.auth.application.mapper.KeycloakUserMapper;
import tech.bytesmind.logistics.auth.application.properties.KeycloakAdminProperties;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing users with hybrid approach (Keycloak + Local DB).
 *
 * Manages user lifecycle:
 * - Create users in Keycloak (identity management)
 * - Sync user profiles to local database
 * - Link via externalAuthId (Keycloak user ID)
 * - Manage user agency assignments and roles locally
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserService {

    private final Keycloak keycloakAdmin;
    private final KeycloakAdminProperties props;
    private final KeycloakUserMapper mapper;
    private final UserRepository userRepository;

    /**
     * Creates a new user in Keycloak and syncs to local database.
     * Links user via externalAuthId (Keycloak user ID).
     *
     * @param request the user creation request
     * @return the created Keycloak user ID
     * @throws KeycloakAdminException if creation fails
     */
    public String createUser(KeycloakCreateUserRequest request) {
        log.debug("Creating user with email: {}", request.getEmail());

        // Check if user already exists in Keycloak
        Optional<UserRepresentation> existing = findByUsername(request.getUsername());
        if (existing.isPresent()) {
            log.warn("User {} already exists in Keycloak", request.getUsername());
            throw new KeycloakAdminException("User " + request.getUsername() + " already exists");
        }

        // Check if user exists in local DB
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("User {} already exists in local DB", request.getEmail());
            throw new KeycloakAdminException("User with email " + request.getEmail() + " already exists");
        }

        // Convert DTO to Keycloak representation
        UserRepresentation userRep = mapper.toKeycloakUser(request);

        // Create user in Keycloak
        String keycloakUserId;
        try (Response response = getUsersResource().create(userRep)) {
            if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
                String errorMsg = response.readEntity(String.class);
                log.error("Keycloak user creation failed with status {}: {}", response.getStatus(), errorMsg);
                throw new KeycloakAdminException(
                        "Failed to create user " + request.getUsername() + ": " + response.getStatus() + " " + errorMsg
                );
            }

            // Extract user ID from Location header
            URI location = response.getLocation();
            keycloakUserId = extractUserIdFromLocation(location);
            if (keycloakUserId == null || keycloakUserId.isBlank()) {
                log.error("Failed to extract user ID from response location: {}", location);
                throw new KeycloakAdminException("Failed to determine created user ID for " + request.getUsername());
            }

            // Set password
            setPassword(keycloakUserId, request.getPassword(), false);

            log.info("Successfully created Keycloak user {} with ID {}", request.getUsername(), keycloakUserId);
        } catch (KeycloakAdminException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error creating user {}: {}", request.getUsername(), ex.getMessage(), ex);
            throw new KeycloakAdminException(
                    "Unexpected error creating user " + request.getUsername() + ": " + ex.getMessage(), ex
            );
        }

        // Sync user profile to local database
        try {
            User localUser = new User();
            localUser.setExternalAuthId(keycloakUserId);  // Link to Keycloak
            localUser.setEmail(request.getEmail());
            localUser.setFirstName(request.getFirstName());
            localUser.setLastName(request.getLastName());
            localUser.setUsername(request.getUsername());
            localUser.setActive(true);
            localUser.setEmailVerified(false);

            userRepository.save(localUser);
            log.info("User profile synced to local database with externalAuthId: {}", keycloakUserId);
        } catch (Exception ex) {
            log.error("Failed to sync user to local database (Keycloak user created): {}", ex.getMessage(), ex);
            // Don't fail if local DB sync fails - Keycloak user is already created
        }

        return keycloakUserId;
    }

    /**
     * Sets or resets a user's password.
     *
     * @param userId the user's UUID in Keycloak
     * @param password the new password
     * @param temporary whether the password is temporary (user must change on first login)
     * @throws KeycloakAdminException if password reset fails
     */
    public void setPassword(String userId, String password, boolean temporary) {
        log.debug("Setting password for user ID: {} (temporary: {})", userId, temporary);

        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(temporary);

            getUsersResource().get(userId).resetPassword(credential);
            log.info("Password set successfully for user ID: {}", userId);
        } catch (Exception ex) {
            log.error("Failed to set password for user ID {}: {}", userId, ex.getMessage(), ex);
            throw new KeycloakAdminException("Failed to set password for user ID " + userId + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Finds a user by username.
     *
     * @param username the username to search for
     * @return Optional containing the user if found
     * @throws KeycloakAdminException if the search fails
     */
    public Optional<UserRepresentation> findByUsername(String username) {
        log.debug("Searching for user by username: {}", username);

        try {
            List<UserRepresentation> users = getUsersResource().search(username, 0, 1);
            if (users.isEmpty()) {
                log.debug("User not found: {}", username);
                return Optional.empty();
            }
            log.debug("User found: {}", username);
            return Optional.of(users.getFirst());
        } catch (Exception ex) {
            log.error("Error searching for user {}: {}", username, ex.getMessage(), ex);
            throw new KeycloakAdminException("Error searching for user " + username + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Finds a user by their UUID.
     *
     * @param userId the user's UUID
     * @return Optional containing the user if found
     * @throws KeycloakAdminException if the lookup fails
     */
    public Optional<UserRepresentation> findById(String userId) {
        log.debug("Searching for user by ID: {}", userId);

        try {
            UserRepresentation user = getUsersResource().get(userId).toRepresentation();
            log.debug("User found by ID: {}", userId);
            return Optional.of(user);
        } catch (NotFoundException ex) {
            log.debug("User not found with ID: {}", userId);
            return Optional.empty();
        } catch (Exception ex) {
            log.error("Error searching for user by ID {}: {}", userId, ex.getMessage(), ex);
            throw new KeycloakAdminException("Error searching for user by ID " + userId + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Deletes a user by their UUID.
     *
     * @param userId the user's UUID
     * @throws KeycloakAdminException if deletion fails
     */
    public void deleteUser(String userId) {
        log.debug("Deleting user with ID: {}", userId);

        try {
            getUsersResource().get(userId).remove();
            log.info("User deleted successfully: {}", userId);
        } catch (Exception ex) {
            log.error("Error deleting user {}: {}", userId, ex.getMessage(), ex);
            throw new KeycloakAdminException("Error deleting user " + userId + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Updates a user's basic information (name, email, etc.).
     * Does NOT change the password; use setPassword() for that.
     *
     * @param userId the user's UUID
     * @param update the updated user representation
     * @throws KeycloakAdminException if the update fails
     */
    public void updateUser(String userId, UserRepresentation update) {
        log.debug("Updating user with ID: {}", userId);

        try {
            getUsersResource().get(userId).update(update);
            log.info("User updated successfully: {}", userId);
        } catch (Exception ex) {
            log.error("Error updating user {}: {}", userId, ex.getMessage(), ex);
            throw new KeycloakAdminException("Error updating user " + userId + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Searches for users by exact or partial username match.
     *
     * @param username the username or partial username to search for
     * @param firstResult the index of the first result (for pagination)
     * @param maxResults the maximum number of results
     * @return list of matching users
     * @throws KeycloakAdminException if the search fails
     */
    public List<UserRepresentation> searchUsers(String username, int firstResult, int maxResults) {
        log.debug("Searching for users with username pattern: {} (offset: {}, limit: {})", username, firstResult, maxResults);

        try {
            List<UserRepresentation> users = getUsersResource().search(username, firstResult, maxResults);
            log.debug("Found {} users matching pattern: {}", users.size(), username);
            return users;
        } catch (Exception ex) {
            log.error("Error searching users with pattern {}: {}", username, ex.getMessage(), ex);
            throw new KeycloakAdminException("Error searching users with pattern " + username + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Lists all users in the target realm with pagination support.
     *
     * @param firstResult the index of the first result (for pagination)
     * @param maxResults the maximum number of results
     * @return list of users
     * @throws KeycloakAdminException if the operation fails
     */
    public List<UserRepresentation> listUsers(int firstResult, int maxResults) {
        log.debug("Listing all users (offset: {}, limit: {})", firstResult, maxResults);

        try {
            List<UserRepresentation> users = getUsersResource().list(firstResult, maxResults);
            log.debug("Retrieved {} users", users.size());
            return users;
        } catch (Exception ex) {
            log.error("Error listing users: {}", ex.getMessage(), ex);
            throw new KeycloakAdminException("Error listing users: " + ex.getMessage(), ex);
        }
    }

    /**
     * Enables or disables a user account.
     *
     * @param userId the user's UUID
     * @param enabled true to enable, false to disable
     * @throws KeycloakAdminException if the operation fails
     */
    public void setUserEnabled(String userId, boolean enabled) {
        log.debug("Setting user {} enabled status to: {}", userId, enabled);

        try {
            UserRepresentation user = getUsersResource().get(userId).toRepresentation();
            user.setEnabled(enabled);
            getUsersResource().get(userId).update(user);
            log.info("User {} enabled status set to: {}", userId, enabled);
        } catch (Exception ex) {
            log.error("Error setting user {} enabled status: {}", userId, ex.getMessage(), ex);
            throw new KeycloakAdminException(
                    "Error setting user " + userId + " enabled status: " + ex.getMessage(), ex
            );
        }
    }

    /**
     * Marks a user's email as verified.
     *
     * @param userId the user's UUID
     * @throws KeycloakAdminException if the operation fails
     */
    public void verifyEmail(String userId) {
        log.debug("Verifying email for user ID: {}", userId);

        try {
            UserRepresentation user = getUsersResource().get(userId).toRepresentation();
            user.setEmailVerified(true);
            getUsersResource().get(userId).update(user);
            log.info("Email verified for user ID: {}", userId);
        } catch (Exception ex) {
            log.error("Error verifying email for user {}: {}", userId, ex.getMessage(), ex);
            throw new KeycloakAdminException("Error verifying email for user " + userId + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Gets user from local database by Keycloak ID.
     *
     * @param keycloakUserId the Keycloak user ID (externalAuthId)
     * @return Optional containing user if found
     */
    public Optional<User> getUserByKeycloakId(String keycloakUserId) {
        log.debug("Fetching user by Keycloak ID: {}", keycloakUserId);
        return userRepository.findByExternalAuthId(keycloakUserId);
    }

    /**
     * Gets user from local database by email.
     *
     * @param email the user email
     * @return Optional containing user if found
     */
    public Optional<User> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Lists all users from local database.
     *
     * @return list of all users
     */
    public List<User> listAllUsers() {
        log.debug("Listing all users from local database");
        return userRepository.findAll();
    }

    /**
     * Updates user profile in local database.
     *
     * @param keycloakUserId the Keycloak user ID
     * @param firstName the first name
     * @param lastName the last name
     * @return updated user
     * @throws KeycloakAdminException if user not found
     */
    public User updateUserProfile(String keycloakUserId, String firstName, String lastName) {
        log.debug("Updating profile for user: {}", keycloakUserId);

        Optional<User> userOpt = userRepository.findByExternalAuthId(keycloakUserId);
        if (userOpt.isEmpty()) {
            throw new KeycloakAdminException("User not found: " + keycloakUserId);
        }

        User user = userOpt.get();
        user.setFirstName(firstName);
        user.setLastName(lastName);

        return userRepository.save(user);
    }

    /**
     * Sets user active status in local database.
     *
     * @param keycloakUserId the Keycloak user ID
     * @param active the active status
     * @return updated user
     */
    public User setUserActive(String keycloakUserId, boolean active) {
        log.debug("Setting active status {} for user: {}", active, keycloakUserId);

        Optional<User> userOpt = userRepository.findByExternalAuthId(keycloakUserId);
        if (userOpt.isEmpty()) {
            throw new KeycloakAdminException("User not found: " + keycloakUserId);
        }

        User user = userOpt.get();
        user.setActive(active);

        return userRepository.save(user);
    }

    /**
     * Sets user agency assignment in local database.
     *
     * @param keycloakUserId the Keycloak user ID
     * @param agencyId the agency ID
     * @return updated user
     */
    public User setUserAgency(String keycloakUserId, UUID agencyId) {
        log.debug("Setting agency {} for user: {}", agencyId, keycloakUserId);

        Optional<User> userOpt = userRepository.findByExternalAuthId(keycloakUserId);
        if (userOpt.isEmpty()) {
            throw new KeycloakAdminException("User not found: " + keycloakUserId);
        }

        User user = userOpt.get();
        user.setAgencyId(agencyId);

        return userRepository.save(user);
    }

    /**
     * Helper: Gets the UsersResource for the target realm.
     *
     * @return UsersResource for the target realm
     */
    private UsersResource getUsersResource() {
        return keycloakAdmin.realm(props.getTargetRealm()).users();
    }

    /**
     * Helper: Extracts user ID from Location header URI.
     * Expected format: /admin/realms/{realm}/users/{userId}
     *
     * @param location the Location URI
     * @return the user ID, or null if not found
     */
    private String extractUserIdFromLocation(URI location) {
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }
        return path.substring(path.lastIndexOf('/') + 1);
    }
}

