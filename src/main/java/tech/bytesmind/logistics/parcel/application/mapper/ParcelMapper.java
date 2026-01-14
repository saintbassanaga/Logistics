package tech.bytesmind.logistics.parcel.application.mapper;

import org.mapstruct.*;
import tech.bytesmind.logistics.parcel.api.dto.CreateParcelRequest;
import tech.bytesmind.logistics.parcel.api.dto.ParcelResponse;
import tech.bytesmind.logistics.parcel.domain.model.Parcel;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ParcelMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agencyId", ignore = true)
    @Mapping(target = "trackingNumber", ignore = true)
    @Mapping(target = "shipment", ignore = true)
    @Mapping(target = "status", constant = "REGISTERED")
    @Mapping(target = "currentLocationId", ignore = true)
    @Mapping(target = "lastScanAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deliveredAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Parcel toEntity(CreateParcelRequest request);

    @Mapping(target = "shipmentId", source = "shipment.id")
    ParcelResponse toResponse(Parcel parcel);

    List<ParcelResponse> toResponseList(List<Parcel> parcels);
}
