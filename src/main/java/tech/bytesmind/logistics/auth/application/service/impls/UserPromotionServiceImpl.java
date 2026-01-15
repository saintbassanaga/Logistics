package tech.bytesmind.logistics.auth.application.service.impls;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.auth.infrastructure.repository.RoleRepository;
import tech.bytesmind.logistics.auth.infrastructure.repository.UserRepository;
import tech.bytesmind.logistics.shared.exceptions.BusinessException;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.service.UserPromotionService;

import java.util.UUID;

/**
 * Implementation of UserPromotionService.
 * Handles promotion of users to agency admin when they create an agency.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserPromotionServiceImpl implements UserPromotionService {

    private static final Logger log = LoggerFactory.getLogger(UserPromotionServiceImpl.class);
    private static final String AGENCY_ADMIN_ROLE = "AGENCY_ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public void promoteToAgencyAdmin(UUID userId, UUID agencyId) {
        log.info("Promoting user {} to AGENCY_ADMIN of agency {}", userId, agencyId);

        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));

        // Validate user can be promoted
        validateUserForPromotion(user);

        // Change actor type from CUSTOMER to AGENCY_EMPLOYEE
        user.setActorType(ActorType.AGENCY_EMPLOYEE);

        // Set the agency ID
        user.setAgencyId(agencyId);

        // Assign AGENCY_ADMIN role
        Role agencyAdminRole = roleRepository.findByCode(AGENCY_ADMIN_ROLE)
                .orElseThrow(() -> new BusinessException("AGENCY_ADMIN role not found. Please seed roles first."));

        user.getRoles().add(agencyAdminRole);

        // Save the updated user
        userRepository.save(user);

        log.info("User {} successfully promoted to AGENCY_ADMIN of agency {}", userId, agencyId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCreateAgency(UUID userId) {
        return userRepository.findById(userId)
                .map(this::isEligibleForAgencyCreation)
                .orElse(false);
    }

    private void validateUserForPromotion(User user) {
        if (!user.isActive()) {
            throw new BusinessException("User account is not active");
        }

        if (user.getActorType() != ActorType.CUSTOMER) {
            throw new BusinessException("Only CUSTOMER users can create and become admin of a new agency. " +
                    "Current actor type: " + user.getActorType());
        }

        if (user.getAgencyId() != null) {
            throw new BusinessException("User already belongs to an agency");
        }
    }

    private boolean isEligibleForAgencyCreation(User user) {
        return user.isActive()
                && user.getActorType() == ActorType.CUSTOMER
                && user.getAgencyId() == null;
    }
}