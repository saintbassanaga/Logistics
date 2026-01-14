package tech.bytesmind.logistics.parcel.application.mapper;

import org.mapstruct.*;
import tech.bytesmind.logistics.parcel.api.dto.CreateShipmentRequest;
import tech.bytesmind.logistics.parcel.api.dto.ShipmentResponse;
import tech.bytesmind.logistics.parcel.domain.model.Parcel;
import tech.bytesmind.logistics.parcel.domain.model.Shipment;

import java.util.Collection;
import java.util.List;

@Mapper(componentModel = "spring", uses = {ParcelMapper.class})
public interface ShipmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agencyId", ignore = true)
    @Mapping(target = "shipmentNumber", ignore = true)
    @Mapping(target = "status", constant = "OPEN")
    @Mapping(target = "totalWeight", ignore = true)
    @Mapping(target = "parcels", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Shipment toEntity(CreateShipmentRequest request);

    @Named("toResponseWithParcels")
    @Mapping(target = "parcelCount", expression = "java(countParcels(shipment.getParcels()))")
    @Mapping(target = "parcels", source = "parcels")
    ShipmentResponse toResponse(Shipment shipment);

    @Named("toResponseWithoutParcels")
    @Mapping(target = "parcelCount", expression = "java(countParcels(shipment.getParcels()))")
    @Mapping(target = "parcels", ignore = true)
    ShipmentResponse toResponseWithoutParcels(Shipment shipment);

    @IterableMapping(qualifiedByName = "toResponseWithoutParcels")
    List<ShipmentResponse> toResponseList(List<Shipment> shipments);

    default int countParcels(Collection<Parcel> parcels) {
        return parcels == null ? 0 : parcels.size();
    }
}
