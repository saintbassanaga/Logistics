package tech.bytesmind.logistics.shared.tenancy.model;

import java.util.UUID;


public class TenantContext {
    
    private static final ThreadLocal<UUID> CURRENT_AGENCY_ID = new ThreadLocal<>();
    
    public static void setCurrentAgencyId(UUID agencyId) {
        CURRENT_AGENCY_ID.set(agencyId);
    }
    
    public static UUID getCurrentAgencyId() {
        return CURRENT_AGENCY_ID.get();
    }
    
    public static void clear() {
        CURRENT_AGENCY_ID.remove();
    }
    
    public static boolean isSet() {
        return CURRENT_AGENCY_ID.get() != null;
    }
}