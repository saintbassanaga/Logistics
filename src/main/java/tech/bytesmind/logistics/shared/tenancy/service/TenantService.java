package tech.bytesmind.logistics.shared.tenancy.service;


import org.springframework.stereotype.Service;
import tech.bytesmind.logistics.shared.exceptions.TenantViolationException;
import tech.bytesmind.logistics.shared.tenancy.model.TenantContext;

import java.util.UUID;

/**
 * Service responsible for managing tenant-specific operations and ensuring multi-tenant context compliance.
 * <p>
 * This service provides methods for retrieving the current agency's tenant ID and validating
 * access based on tenant context. It relies on {@link TenantContext} to collect tenant-specific information
 * tied to the current thread and enforces security by throwing {@link TenantViolationException} when violations occur.
 * <p>
 * Responsibilities:
 * - Retrieve the current agency's tenant ID from the context.
 * - Validate tenant access to ensure compatibility between the requested agency ID and the agency ID
 *   from the current tenant context.
 * <p>
 * Exceptions:
 * - {@link TenantViolationException}: Thrown when no tenant context exists or when there is a mismatch
 *   between the expected and provided tenant IDs.
 * <p>
 * This service is intended to support multi-tenancy in applications where operations must respect
 * the boundaries of tenant-specific data and permissions.
 */
@Service
public class TenantService {

    public UUID getCurrentAgencyId() {
        UUID agencyId = TenantContext.getCurrentAgencyId();
        if (agencyId == null) {
            throw new TenantViolationException("No tenant context found");
        }
        return agencyId;
    }

    public void validateAgencyAccess(UUID agencyId) {
        UUID currentAgencyId = getCurrentAgencyId();
        if (!currentAgencyId.equals(agencyId)) {
            throw new TenantViolationException(
                "Tenant mismatch: expected " + currentAgencyId + " but got " + agencyId
            );
        }
    }
}