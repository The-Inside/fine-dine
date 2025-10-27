package com.finedine.customerservice.util;

import com.finedine.customerservice.dto.CustomerCreationQueueObject;
import com.finedine.customerservice.dto.CustomerResponse;
import com.finedine.customerservice.dto.CustomerUpdateRequest;
import com.finedine.customerservice.entity.Customer;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerMapper {

    CustomerResponse toCustomerResponse(Customer customer);

    @Mapping(target = "address", source = "address", qualifiedByName = "stringToList")
    Customer toCustomer(CustomerCreationQueueObject data);

    @Mapping(target = "address", ignore = true)
    void updateCustomerFromRequest(CustomerUpdateRequest request, @MappingTarget Customer customer);
    @Named("stringToList")
    default List<String> mapStringToList(String address) {
        return (address != null && !address.isBlank())
                ? List.of(address)
                : new ArrayList<>();
    }

}