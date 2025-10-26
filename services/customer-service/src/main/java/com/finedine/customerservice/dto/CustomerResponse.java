package com.finedine.customerservice.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CustomerResponse(
        String externalId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        List<String> address,
        String profilePictureUrl
) {
}
