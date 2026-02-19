package tech.bytesmind.logistics.auth.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.auth.api.dto.CreateUserRequest;
import tech.bytesmind.logistics.auth.api.dto.UpdateUserRequest;
import tech.bytesmind.logistics.auth.domain.event.PasswordChangedEvent;
import tech.bytesmind.logistics.auth.domain.event.UserCreatedEvent;
import tech.bytesmind.logistics.auth.domain.event.UserDeactivatedEvent;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.auth.domain.service.UserDomainService;
import tech.bytesmind.logistics.auth.infrastructure.repository.RoleRepository;
import tech.bytesmind.logistics.auth.infrastructure.repository.UserRepository;
import tech.bytesmind.logistics.auth.application.mapper.UserMapper;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;
import tech.bytesmind.logistics.shared.event.publisher.TransactionalEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Application service for administrative user management.
 *
 * <p>All persistence is local — no external identity provider is called.
 * Passwords are BCrypt-hashed before storage.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Admin CRUD operations on {@code platform_user}</li>
 *   <li>Role assignment / revocation</li>
 *   <li>Account activation / deactivation</li>
 *   <li>Email verification</li>
 *   <li>Password reset (admin-initiated)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDomainService userDomainService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final TransactionalEventPublisher eventPublisher;

    // =========================================================================
    // Queries
    // =========================================================================

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new BusinessException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<User> listAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> listByAgency(UUID agencyId) {
        return userRepository.findByAgencyId(agencyId);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // =========================================================================
    // Create
    // =========================================================================

    /**
     * Creates a new user.
     *
     * @param request     user data
     * @param rawPassword optional initial password (null → user cannot log in until password is set)
     */
    public User createUser(CreateUserRequest request, String rawPassword) {
        log.debug("Creating user: email={}, actorType={}", request.email(), request.actorType());

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered: " + request.email());
        }
        if (request.username() != null && userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already taken: " + request.username());
        }

        User user = userMapper.toEntity(request);
        user.setActorType(request.actorType());
        user.setAgencyId(request.agencyId());
        user.setJobTitle(request.jobTitle());
        user.setDepartment(request.department());

        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }

        userDomainService.validateUser(user);

        User saved = userRepository.save(user);
        eventPublisher.publish(
                new UserCreatedEvent(saved.getId(), saved.getEmail(), saved.getActorType(), saved.getAgencyId())
        );

        log.info("User created: id={}, email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    // =========================================================================
    // Update
    // =========================================================================

    public User updateUser(UUID id, UpdateUserRequest request) {
        log.debug("Updating user: id={}", id);
        User user = findById(id);
        userMapper.updateEntity(request, user);
        User saved = userRepository.save(user);
        log.info("User updated: id={}", id);
        return saved;
    }

    // =========================================================================
    // Activation / Deactivation
    // =========================================================================

    public User activateUser(UUID id) {
        User user = findById(id);
        userDomainService.activate(user);
        return userRepository.save(user);
    }

    public User deactivateUser(UUID id) {
        User user = findById(id);
        userDomainService.deactivate(user);
        User saved = userRepository.save(user);
        eventPublisher.publish(new UserDeactivatedEvent(saved.getId(), "admin_deactivation", saved.getAgencyId()));
        log.info("User deactivated: id={}", id);
        return saved;
    }

    // =========================================================================
    // Email verification
    // =========================================================================

    public User verifyEmail(UUID id) {
        User user = findById(id);
        user.setEmailVerified(true);
        User saved = userRepository.save(user);
        log.info("Email verified for user: id={}", id);
        return saved;
    }

    // =========================================================================
    // Password management
    // =========================================================================

    /**
     * Admin-initiated password reset. Forces the new password without checking the current one.
     */
    public void resetPassword(UUID id, String newRawPassword) {
        User user = findById(id);
        userDomainService.validateCanUpdatePassword(user);
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
        eventPublisher.publish(new PasswordChangedEvent(user.getId(), user.getEmail()));
        log.info("Password reset for user: id={}", id);
    }

    // =========================================================================
    // Role management
    // =========================================================================

    public User assignRole(UUID userId, UUID roleId) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Role not found: " + roleId));

        userDomainService.assignRole(user, role);
        return userRepository.save(user);
    }

    public User revokeRole(UUID userId, UUID roleId) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Role not found: " + roleId));

        userDomainService.revokeRole(user, role);
        return userRepository.save(user);
    }

    // =========================================================================
    // Delete (soft)
    // =========================================================================

    public void deleteUser(UUID id) {
        User user = findById(id);
        // SQLDelete annotation on User entity will perform a soft-delete UPDATE
        userRepository.delete(user);
        log.info("User soft-deleted: id={}", id);
    }

    // =========================================================================
    // Last login tracking
    // =========================================================================

    public void recordLogin(UUID id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        });
    }
}
