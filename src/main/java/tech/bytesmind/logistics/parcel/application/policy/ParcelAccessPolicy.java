package tech.bytesmind.logistics.parcel.application.policy;

import org.springframework.stereotype.Component;
import tech.bytesmind.logistics.parcel.domain.model.Parcel;
import tech.bytesmind.logistics.shared.exceptions.SecurityViolationException;
import tech.bytesmind.logistics.shared.security.model.ActorType;
import tech.bytesmind.logistics.shared.security.model.SecurityContext;

import java.util.UUID;

/**
 * Politique d'accès pour les Parcels (ABAC).
 * Implémente les règles d'autorisation sur les ressources.
 */
@Component
public class ParcelAccessPolicy {

    /**
     * Vérifie si l'acteur peut accéder à un Parcel.
     */
    public boolean canAccess(SecurityContext context, UUID parcelAgencyId) {
        // Platform Admin : accès à tous les parcels
        if (context.actorType() == ActorType.PLATFORM_ADMIN) {
            return true;
        }

        // Agency Employee : accès uniquement aux parcels de son agence
        if (context.actorType() == ActorType.AGENCY_EMPLOYEE) {
            if (context.agencyId() == null) {
                return false;
            }
            return context.agencyId().equals(parcelAgencyId);
        }

        // Customer : pourrait accéder à ses propres parcels (à implémenter avec sender/receiver ID)
        return false;
    }

    /**
     * Vérifie si l'acteur peut créer un Parcel.
     */
    public boolean canCreate(SecurityContext context) {
        // Platform Admin et Agency Employee peuvent créer des parcels
        return context.actorType() == ActorType.PLATFORM_ADMIN ||
                context.actorType() == ActorType.AGENCY_EMPLOYEE;
    }

    /**
     * Vérifie si l'acteur peut modifier un Parcel.
     */
    public boolean canModify(SecurityContext context, Parcel parcel) {
        // Platform Admin : modification complète
        if (context.actorType() == ActorType.PLATFORM_ADMIN) {
            return true;
        }

        // Agency Employee : modification uniquement si appartient à son agence
        if (context.actorType() == ActorType.AGENCY_EMPLOYEE) {
            if (context.agencyId() == null || !context.agencyId().equals(parcel.getAgencyId())) {
                return false;
            }
            // Requiert le rôle PARCEL_MANAGER ou AGENCY_ADMIN
            return context.hasRole("PARCEL_MANAGER") || context.hasRole("AGENCY_ADMIN");
        }

        return false;
    }

    /**
     * Vérifie si l'acteur peut changer le statut d'un Parcel.
     */
    public boolean canUpdateStatus(SecurityContext context, Parcel parcel) {
        // Platform Admin : mise à jour complète
        if (context.actorType() == ActorType.PLATFORM_ADMIN) {
            return true;
        }

        // Agency Employee : mise à jour si appartient à son agence avec rôle approprié
        if (context.actorType() == ActorType.AGENCY_EMPLOYEE) {
            if (context.agencyId() == null || !context.agencyId().equals(parcel.getAgencyId())) {
                return false;
            }
            // Les opérateurs de tri et livreurs peuvent mettre à jour le statut
            return context.hasRole("PARCEL_MANAGER") ||
                    context.hasRole("SORTING_OPERATOR") ||
                    context.hasRole("DELIVERY_DRIVER") ||
                    context.hasRole("AGENCY_ADMIN");
        }

        return false;
    }

    /**
     * Vérifie si l'acteur peut tracker un Parcel (lecture seule étendue).
     */
    public boolean canTrack(SecurityContext context, UUID parcelAgencyId) {
        // Tout le monde peut tracker (avec des restrictions sur les données sensibles)
        // Pour l'instant, même logique que canAccess
        return canAccess(context, parcelAgencyId);
    }

    /**
     * Vérifie si l'acteur peut supprimer un Parcel.
     */
    public boolean canDelete(SecurityContext context, Parcel parcel) {
        // Seul Platform Admin et AGENCY_ADMIN peuvent supprimer
        if (context.actorType() == ActorType.PLATFORM_ADMIN) {
            return true;
        }

        if (context.actorType() == ActorType.AGENCY_EMPLOYEE) {
            if (context.agencyId() == null || !context.agencyId().equals(parcel.getAgencyId())) {
                return false;
            }
            return context.hasRole("AGENCY_ADMIN");
        }

        return false;
    }

    /**
     * Valide l'accès ou lance une exception.
     */
    public void validateAccess(SecurityContext context, UUID parcelAgencyId) {
        if (!canAccess(context, parcelAgencyId)) {
            throw new SecurityViolationException(
                    "Access denied to parcel for actor " + context.userId()
            );
        }
    }

    /**
     * Valide la permission de modification ou lance une exception.
     */
    public void validateModify(SecurityContext context, Parcel parcel) {
        if (!canModify(context, parcel)) {
            throw new SecurityViolationException(
                    "Modification denied for parcel " + parcel.getId()
            );
        }
    }

    /**
     * Valide la permission de mise à jour du statut ou lance une exception.
     */
    public void validateUpdateStatus(SecurityContext context, Parcel parcel) {
        if (!canUpdateStatus(context, parcel)) {
            throw new SecurityViolationException(
                    "Status update denied for parcel " + parcel.getId()
            );
        }
    }
}