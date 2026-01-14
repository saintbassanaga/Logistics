package tech.bytesmind.logistics.auth.application.service.impls;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.auth.api.dto.CreateUserRequest;
import tech.bytesmind.logistics.auth.api.dto.UpdateUserRequest;
import tech.bytesmind.logistics.auth.application.mapper.UserMapper;
import tech.bytesmind.logistics.auth.application.service.UserService;
import tech.bytesmind.logistics.auth.domain.event.*;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.auth.domain.service.UserDomainService;
import tech.bytesmind.logistics.auth.infrastructure.repository.RoleRepository;
import tech.bytesmind.logistics.auth.infrastructure.repository.UserRepository;
import tech.bytesmind.logistics.shared.event.publisher.TransactionalEventPublisher;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;

import java.util.List;
import java.util.UUID;

/**
 * Implémentation du service applicatif pour les Users.
 * Orchestration de la logique métier et publication d'événements.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDomainService userDomainService;
    private final UserMapper userMapper;
    private final TransactionalEventPublisher eventPublisher;

    @Override
    @Transactional
    public User createUser(CreateUserRequest request) {
        log.info("Creating user: {}", request.email());

        // Convertir DTO → Entity via UserMapper
        User user = userMapper.toEntity(request);

        // Valider l'entité via le service de domaine
        userDomainService.validateUser(user);

        // Vérifier unicité email
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("User with email '" + request.email() + "' already exists");
        }

        // Sauvegarder
        User saved = userRepository.save(user);

        // Publier événement
        eventPublisher.publish(new UserCreatedEvent(
                saved.getId(),
                saved.getEmail(),
                saved.getActorType(),
                saved.getAgencyId()
        ));

        log.info("User created successfully: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserWithRoles(UUID id) {
        return userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new BusinessException("User not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByExternalAuthId(String externalAuthId) {
        return userRepository.findByExternalAuthId(externalAuthId)
                .orElseThrow(() -> new BusinessException("User not found: " + externalAuthId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listUsersByAgency(UUID agencyId) {
        return userRepository.findAgencyEmployees(agencyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listActiveAgencyEmployees(UUID agencyId) {
        return userRepository.findByAgencyIdAndActiveTrue(agencyId);
    }

    @Override
    @Transactional
    public User assignRole(UUID userId, UUID roleId) {
        log.info("Assigning role {} to user {}", roleId, userId);

        User user = getUserById(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Role not found: " + roleId));

        userDomainService.assignRole(user, role);
        User saved = userRepository.save(user);

        // Publier événement
        eventPublisher.publish(new RoleAssignedEvent(
                user.getId(),
                role.getId(),
                role.getCode(),
                user.getAgencyId()
        ));

        log.info("Role assigned successfully");
        return saved;
    }

    @Override
    @Transactional
    public User removeRole(UUID userId, UUID roleId) {
        log.info("Removing role {} from user {}", roleId, userId);

        User user = getUserById(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Role not found: " + roleId));

        userDomainService.revokeRole(user, role);
        User saved = userRepository.save(user);

        // Publier événement
        eventPublisher.publish(new RoleRevokedEvent(
                user.getId(),
                role.getId(),
                role.getCode(),
                user.getAgencyId()
        ));

        log.info("Role removed successfully");
        return saved;
    }

    @Override
    @Transactional
    public void deactivateUser(UUID userId) {
        log.info("Deactivating user: {}", userId);

        User user = getUserById(userId);
        userDomainService.deactivate(user);
        userRepository.save(user);

        // Publier événement
        eventPublisher.publish(new UserDeactivatedEvent(
                user.getId(),
                null, // Reason removed from signature
                user.getAgencyId()
        ));

        log.info("User deactivated successfully");
    }

    @Override
    @Transactional
    public void activateUser(UUID userId) {
        log.info("Activating user: {}", userId);

        User user = getUserById(userId);
        userDomainService.activate(user);
        userRepository.save(user);

        log.info("User activated successfully");
    }

    @Override
    @Transactional
    public void changeUserAgency(UUID userId, UUID newAgencyId) {
        log.info("Changing user {} agency to {}", userId, newAgencyId);

        User user = getUserById(userId);
        UUID oldAgencyId = user.getAgencyId();

        userDomainService.changeAgency(user, newAgencyId);
        userRepository.save(user);

        // Publier événement (très important pour invalidation JWT)
        eventPublisher.publish(new UserAgencyChangedEvent(
                user.getId(),
                oldAgencyId,
                newAgencyId
        ));

        log.info("User agency changed successfully");
    }

    @Override
    @Transactional
    public User updateUser(UUID userId, UpdateUserRequest request) {
        log.info("Updating user: {}", userId);

        User existing = getUserById(userId);

        // Utiliser le mapper pour mettre à jour les champs modifiables
        userMapper.updateEntity(request, existing);

        // Valider après mise à jour
        userDomainService.validateUser(existing);

        User updated = userRepository.save(existing);
        log.info("User updated: {}", userId);

        return updated;
    }
}