package com.finedine.customerservice.controller;

import com.finedine.customerservice.dto.CustomerResponse;
import com.finedine.customerservice.dto.CustomerUpdateRequest;
import com.finedine.customerservice.entity.Customer;
import com.finedine.customerservice.security.SecurityUser;
import com.finedine.customerservice.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.OK)
    public Customer myProfile(@AuthenticationPrincipal SecurityUser securityUser) {
        return customerService.myProfile(securityUser);
    }

    @PatchMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.OK)
    public CustomerResponse profileUpdate(@AuthenticationPrincipal SecurityUser securityUser,
                                          @RequestBody CustomerUpdateRequest request) {
        return customerService.profileSettings(securityUser, request);
    }
}
