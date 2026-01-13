package tech.bytesmind.logistics.architecture;


public final class ArchitectureConstants {
    
    private ArchitectureConstants() {}
    
    // Bounded Contexts
    public static final String CONTEXT_AUTH = "tech.bytesmind.logistics.auth";
    public static final String CONTEXT_AGENCY = "tech.bytesmind.logistics.agency";
    public static final String CONTEXT_PARCEL = "tech.bytesmind.logistics.parcel";
    public static final String CONTEXT_SHARED = "tech.bytesmind.logistics.shared";
    
    // Layers (DDD)
    public static final String LAYER_API = "api";
    public static final String LAYER_APPLICATION = "application";
    public static final String LAYER_DOMAIN = "domain";
    public static final String LAYER_INFRASTRUCTURE = "infrastructure";
    
    // JWT Claims
    public static final String JWT_CLAIM_ACTOR_TYPE = "actor_type";
    public static final String JWT_CLAIM_AGENCY_ID = "agency_id";
    public static final String JWT_CLAIM_ROLES = "roles";
}