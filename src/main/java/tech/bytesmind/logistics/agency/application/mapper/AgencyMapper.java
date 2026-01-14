package tech.bytesmind.logistics.agency.application.mapper;


import org.mapstruct.*;
import tech.bytesmind.logistics.agency.api.dto.AgencyResponse;
import tech.bytesmind.logistics.agency.api.dto.CreateAgencyRequest;
import tech.bytesmind.logistics.agency.domain.model.Agency;
import tech.bytesmind.logistics.agency.domain.model.AgencyLocation;

import java.util.Collection;
import java.util.List;


@Mapper(componentModel = "spring", uses = {LocationMapper.class})
public interface AgencyMapper {

    @Mapping(target = "active", constant = "true")
    @Mapping(target = "agencyId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "suspended", constant = "false")
    @Mapping(target = "suspensionReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "agencyLocations", ignore = true)
    Agency toEntity(CreateAgencyRequest request);

    @Named("toResponseWithLocations")
    @Mapping(target = "locationCount", expression = "java((long)agency.getAgencyLocations().size())")
    @Mapping(target = "activeLocationCount", expression = "java(countActiveLocations(agency.getAgencyLocations()))")
    @Mapping(target = "locations", source = "agencyLocations")
    AgencyResponse toResponse(Agency agency);

    @Named("toResponseWithoutLocations")
    @Mapping(target = "locationCount", constant = "0L")
    @Mapping(target = "activeLocationCount", constant = "0L")
    @Mapping(target = "locations", ignore = true)
    AgencyResponse toResponseWithoutLocations(Agency agency);

    @IterableMapping(qualifiedByName = "toResponseWithoutLocations")
    List<AgencyResponse> toResponseList(List<Agency> agencies);

    default long countActiveLocations(Collection<AgencyLocation> agencyLocations) {
        return agencyLocations == null ? 0 : agencyLocations.stream().filter(AgencyLocation::isActive).count();
    }
}