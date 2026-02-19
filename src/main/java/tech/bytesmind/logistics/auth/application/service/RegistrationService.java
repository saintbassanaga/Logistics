package tech.bytesmind.logistics.auth.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.auth.api.dto.UserRegistrationRequest;
import tech.bytesmind.logistics.auth.api.dto.UserRegistrationResponse;
import tech.bytesmind.logistics.auth.domain.event.UserRegisteredEvent;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.auth.domain.service.UserDomainService;
import tech.bytesmind.logistics.auth.infrastructure.repository.RoleRepository;
import tech.bytesmind.logistics.auth.infrastructure.repository.UserRepository;
import tech.bytesmind.logistics.shared.event.publisher.TransactionalEventPublisher;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

/**
 * Self-registration service for new platform users.
 *
 * <p>Every registered user is created as a {@code CUSTOMER} with the {@code USER} role.
 * Password is BCrypt-hashed before persistence; the plain-text value is never stored.
 *
 * <p>Email verification is required before login (enforced by {@link tech.bytesmind.logistics.shared.security.service.UserDetailsServiceImpl}).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RegistrationService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDomainService userDomainService;
    private final TransactionalEventPublisher eventPublisher;

    public UserRegistrationResponse register(UserRegistrationRequest request) {
        log.debug("Registration request for email={}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already taken");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        userDomainService.initializeForRegistration(user);
        userDomainService.validateUser(user);

        // Assign the default USER role (CUSTOMER-scoped)
        roleRepository.findByCode(DEFAULT_ROLE)
                .ifPresent(role -> user.getRoles().add(role));

        User saved = userRepository.save(user);

        eventPublisher.publish(new UserRegisteredEvent(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getActorType().name()
        ));

        log.info("User registered: id={}, username={}", saved.getId(), saved.getUsername());

        return new UserRegistrationResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getPhone(),
                saved.getActorType().name(),
                saved.getRoleCodes(),
                saved.isActive(),
                saved.isEmailVerified(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
}
