package tech.bytesmind.logistics.agency.infrastructure.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.bytesmind.logistics.agency.domain.model.Agency;
import tech.bytesmind.logistics.agency.domain.model.SubscriptionTier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'entité Agency.
 */
@Repository
public interface AgencyRepository extends JpaRepository<Agency, UUID> {

    Optional<Agency> findByCode(String code);

    Optional<Agency> findByEmail(String email);

    boolean existsByCode(String code);

    boolean existsByEmail(String email);

    List<Agency> findByActiveTrue();

    List<Agency> findByCountry(String country);

    List<Agency> findBySubscriptionTier(SubscriptionTier tier);

    @Query("SELECT a FROM Agency a WHERE a.active = true AND a.suspended = false")
    List<Agency> findAllOperational();

    @Query("SELECT a FROM Agency a LEFT JOIN FETCH a.agencyLocations WHERE a.id = :id")
    Optional<Agency> findByIdWithLocations(@Param("id") UUID id);

    /**
     * Compte les agences dont le code commence par un préfixe donné.
     * Utilisé par AgencyCodeGenerator pour la génération séquentielle.
     */
    long countByCodeStartingWith(String prefix);
}
