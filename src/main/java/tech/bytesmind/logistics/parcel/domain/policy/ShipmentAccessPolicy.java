package tech.bytesmind.logistics.parcel.domain.policy;

import org.springframework.stereotype.Component;
import tech.bytesmind.logistics.parcel.domain.model.Shipment;
import tech.bytesmind.logistics.shared.exceptions.SecurityViolationException;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.model.SecurityContext;

import java.util.UUID;

/**
 * Politique d'accès pour les Shipments (ABAC).
 * Implémente les règles d'autorisation sur les ressources.
 */
@Component
public class ShipmentAccessPolicy {

    /**
     * Vérifie si l'acteur peut accéder à un Shipment.
     */
    public boolean canAccess(SecurityContext context, UUID shipmentAgencyId) {
        // Platform Admin : accès à tous les shipments
        if (context.actorType() == ActorType.PLATFORM_ADMIN) {
            return true;
        }

        // Agency Employee : accès uniquement aux shipments de son agence
        if (context.actorType() == ActorType.AGENCY_EMPLOYEE) {
            if (context.agencyId() == null) {
                return false;
            }
            return context.agencyId().equals(shipmentAgencyId);
        }

        // Customer : pas d'accès direct aux shipments
        return false;
    }

    /**
     * Vérifie si l'acteur peut créer un Shipment.
     */
    public boolean canCreate(SecurityContext context) {
        // Platform Admin et Agency Employee peuvent créer des shipments
        return context.actorType() == ActorType.PLATFORM_ADMIN ||
                context.actorType() == ActorType.AGENCY_EMPLOYEE;
    }

    /**
     * Vérifie si l'acteur peut modifier un Shipment.
     */
    public boolean canModify(SecurityContext context, Shipment shipment) {
        // Platform Admin : modification complète
        if (context.actorType() == ActorType.PLATFORM_ADMIN) {
            return true;
        }

        // Agency Employee : modification uniquement si appartient à son agence
        if (context.actorType() == ActorType.AGENCY_EMPLOYEE) {
            if (context.agencyId() == null || !context.agencyId().equals(shipment.getAgencyId())) {
                return false;
            }
            // Requiert le rôle SHIPMENT_MANAGER ou AGENCY_ADMIN
            return context.hasRole("SHIPMENT_MANAGER") || context.hasRole("AGENCY_ADMIN");
        }

        return false;
    }

    /**
     * Vérifie si l'acteur peut confirmer un Shipment.
     */
    public boolean canConfirm(SecurityContext context, Shipment shipment) {
        // Platform Admin : confirmation complète
        if (context.actorType() == ActorType.PLATFORM_ADMIN) {
            return true;
        }

        // Agency Employee : confirmation si appartient à son agence avec rôle approprié
        if (context.actorType() == ActorType.AGENCY_EMPLOYEE) {
            if (context.agencyId() == null || !context.agencyId().equals(shipment.getAgencyId())) {
                return false;
            }
            return context.hasRole("SHIPMENT_MANAGER") || context.hasRole("AGENCY_ADMIN");
        }

        return false;
    }

    /**
     * Vérifie si l'acteur peut supprimer un Shipment.
     */
    public boolean canDelete(SecurityContext context, Shipment shipment) {
        // Seul Platform Admin et AGENCY_ADMIN peuvent supprimer
        if (context.actorType() == ActorType.PLATFORM_ADMIN) {
            return true;
        }

        if (context.actorType() == ActorType.AGENCY_EMPLOYEE) {
            if (context.agencyId() == null || !context.agencyId().equals(shipment.getAgencyId())) {
                return false;
            }
            return context.hasRole("AGENCY_ADMIN");
        }

        return false;
    }

    /**
     * Valide l'accès ou lance une exception.
     */
    public void validateAccess(SecurityContext context, UUID shipmentAgencyId) {
        if (!canAccess(context, shipmentAgencyId)) {
            throw new SecurityViolationException(
                    "Access denied to shipment for actor " + context.userId()
            );
        }
    }

    /**
     * Valide la permission de modification ou lance une exception.
     */
    public void validateModify(SecurityContext context, Shipment shipment) {
        if (!canModify(context, shipment)) {
            throw new SecurityViolationException(
                    "Modification denied for shipment " + shipment.getId()
            );
        }
    }

    /**
     * Valide la permission de confirmation ou lance une exception.
     */
    public void validateConfirm(SecurityContext context, Shipment shipment) {
        if (!canConfirm(context, shipment)) {
            throw new SecurityViolationException(
                    "Confirmation denied for shipment " + shipment.getId()
            );
        }
    }
}