package tech.bytesmind.logistics.shared.tenancy.model;

import java.util.UUID;


public interface TenantAware {
    UUID getAgencyId();
    void setAgencyId(UUID agencyId);
}