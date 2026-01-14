package tech.bytesmind.logistics.agency.application.mapper;


import org.mapstruct.*;
import tech.bytesmind.logistics.agency.api.dto.CreateLocationRequest;
import tech.bytesmind.logistics.agency.api.dto.LocationResponse;
import tech.bytesmind.logistics.agency.domain.model.AgencyLocation;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agencyId", ignore = true)
    @Mapping(target = "agency", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "temporarilyClosed", constant = "false")
    @Mapping(target = "closureReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    AgencyLocation toEntity(CreateLocationRequest request);

    @Mapping(target = "operational", expression = "java(location.isActive() && !location.isTemporarilyClosed())")
    LocationResponse toResponse(AgencyLocation location);

    List<LocationResponse> toResponseList(List<AgencyLocation> locations);
}
