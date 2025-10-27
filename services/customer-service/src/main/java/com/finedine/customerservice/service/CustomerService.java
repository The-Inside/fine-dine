package com.finedine.customerservice.service;

import com.finedine.customerservice.dto.CustomerCreationQueueObject;
import com.finedine.customerservice.dto.CustomerResponse;
import com.finedine.customerservice.dto.CustomerUpdateRequest;
import com.finedine.customerservice.dto.OrderResponse;
import com.finedine.customerservice.entity.Customer;
import com.finedine.customerservice.security.SecurityUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    /**
     * Create a new customer based on the data received from the customer creation queue.
     *
     * @param data The data required to create a new customer.
     */
    void createCustomer(CustomerCreationQueueObject data);

    /**
     * Retrieve the profile of the currently authenticated customer.
     *
     * @param securityUser The security user representing the authenticated customer.
     * @return The customer's profile information.
     */
    Customer myProfile(SecurityUser securityUser);

    /**
     * Update the profile settings of the currently authenticated customer.
     *
     * @param securityUser The security user representing the authenticated customer.
     * @param updatedCustomerData The updated customer data.
     * @return The updated customer's profile information.
     */
    CustomerResponse profileSettings(SecurityUser securityUser, CustomerUpdateRequest updatedCustomerData);

    /**
     * Retrieve a paginated list of orders for the currently authenticated customer.
     *
     * @param securityUser The security user representing the authenticated customer.
     * @param pageable Pagination information.
     * @return A paginated list of the customer's orders.
     */
    Page<OrderResponse> getCustomerOrders(SecurityUser securityUser, Pageable pageable);

}
