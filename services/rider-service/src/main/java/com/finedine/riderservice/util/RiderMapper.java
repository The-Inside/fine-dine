package com.finedine.riderservice.util;

import com.finedine.riderservice.dto.DeliveryRequestDTO;
import com.finedine.riderservice.dto.RiderRegistrationQueue;
import com.finedine.riderservice.dto.RiderResponse;
import com.finedine.riderservice.entity.Delivery;
import com.finedine.riderservice.entity.Rider;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RiderMapper {

    Rider toRider(RiderRegistrationQueue riderResponse);

    RiderResponse toRiderResponse(Rider rider);

    Delivery toDelivery(DeliveryRequestDTO requestDTO);
}
