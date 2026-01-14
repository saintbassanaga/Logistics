package tech.bytesmind.logistics.agency.infrastructure.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.bytesmind.logistics.agency.domain.model.AgencyLocation;
import tech.bytesmind.logistics.agency.domain.model.LocationType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'entité AgencyLocation.
 */
@Repository
public interface AgencyLocationRepository extends JpaRepository<AgencyLocation, UUID> {

    List<AgencyLocation> findByAgencyId(UUID agencyId);

    List<AgencyLocation> findByAgencyIdAndActiveTrue(UUID agencyId);

    @Query("SELECT l FROM AgencyLocation l WHERE l.agency.id  = :agencyId " +
            "AND l.active = true AND l.temporarilyClosed = false")
    List<AgencyLocation> findOperationalByAgencyId(@Param("agencyId") UUID agencyId);

    Optional<AgencyLocation> findByAgencyIdAndCode(UUID agencyId, String code);

    boolean existsByAgencyIdAndCode(UUID agencyId, String code);

    List<AgencyLocation> findByAgencyIdAndLocationType(UUID agencyId, LocationType locationType);

    List<AgencyLocation> findByAgencyIdAndCity(UUID agencyId, String city);

    List<AgencyLocation> findAgencyLocationByAgencyIdAndLocationTypeAndActive_TrueOrTemporarilyClosed_False(UUID agencyId, LocationType locationType);

    long countByAgencyId(UUID agencyId);

    long countByAgencyIdAndActiveTrue(UUID agencyId);
}
