package com.finedine.customerservice.dto;

public record CustomerUpdateRequest(
        String firstName,
        String lastName,
        String phoneNumber,
        String address) {
}
