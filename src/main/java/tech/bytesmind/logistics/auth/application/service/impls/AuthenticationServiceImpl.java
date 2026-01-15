package tech.bytesmind.logistics.auth.application.service.impls;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.auth.api.dto.PasswordUpdateRequest;
import tech.bytesmind.logistics.auth.api.dto.UserProfileUpdateRequest;
import tech.bytesmind.logistics.auth.api.dto.UserRegistrationRequest;
import tech.bytesmind.logistics.auth.application.service.AuthenticationService;
import tech.bytesmind.logistics.auth.domain.event.PasswordChangedEvent;
import tech.bytesmind.logistics.auth.domain.event.UserRegisteredEvent;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.auth.domain.service.UserDomainService;
import tech.bytesmind.logistics.auth.infrastructure.repository.RoleRepository;
import tech.bytesmind.logistics.auth.infrastructure.repository.UserRepository;
import tech.bytesmind.logistics.shared.event.publisher.TransactionalEventPublisher;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

import java.time.Instant;
import java.util.UUID;

/**
 * Implémentation du service d'authentification.
 * Gère l'inscription des utilisateurs et la gestion du profil.
 * L'authentification JWT est déléguée à Keycloak.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    private static final String DEFAULT_CUSTOMER_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDomainService userDomainService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionalEventPublisher eventPublisher;

    @Override
    public User registerUser(UserRegistrationRequest request) {
        log.info("Registering new user with email: {}", request.email());

        // Vérifier l'unicité de l'email et du username
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email is already registered");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username is already taken");
        }

        // Valider le username
        userDomainService.validateUsername(request.username());

        // Créer l'utilisateur
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());

        // Initialiser pour l'inscription (CUSTOMER, active, emailVerified=false)
        userDomainService.initializeForRegistration(user);

        // Valider l'utilisateur
        userDomainService.validateUser(user);

        // Sauvegarder l'utilisateur
        user = userRepository.save(user);

        // Assigner le rôle USER par défaut
        Role userRole = roleRepository.findByCode(DEFAULT_CUSTOMER_ROLE)
                .orElseThrow(() -> new BusinessException("Default USER role not found. Please run database migrations."));

        userDomainService.assignRole(user, userRole);
        user = userRepository.save(user);

        // Publier l'événement
        eventPublisher.publish(new UserRegisteredEvent(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getActorType().name()
        ));

        log.info("User registered successfully: {} ({})", user.getUsername(), user.getId());
        return user;
    }

    @Override
    public void updatePassword(UUID userId, PasswordUpdateRequest request) {
        log.info("Updating password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Valider que l'utilisateur peut mettre à jour son mot de passe
        userDomainService.validateCanUpdatePassword(user);

        // Vérifier le mot de passe actuel
        if (user.getPasswordHash() == null) {
            throw new BusinessException("Password not set for this account. Please use Keycloak to set your password.");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        // Mettre à jour le mot de passe
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Publier l'événement
        eventPublisher.publish(new PasswordChangedEvent(
                user.getId(),
                user.getEmail()
        ));

        log.info("Password updated successfully for user: {}", userId);
    }

    @Override
    public User updateProfile(UUID userId, UserProfileUpdateRequest request) {
        log.info("Updating profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Mettre à jour les champs modifiables
        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName());
        }

        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName());
        }

        if (request.phone() != null) {
            user.setPhone(request.phone());
        }

        // Valider et sauvegarder
        userDomainService.validateUser(user);
        user = userRepository.save(user);

        log.info("Profile updated successfully for user: {}", userId);
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUserProfile(UUID userId) {
        return userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public void recordLogin(UUID userId) {
        log.debug("Recording login for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
    }
}