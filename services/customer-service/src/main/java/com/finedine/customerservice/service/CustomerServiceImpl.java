package com.finedine.customerservice.service;

import com.finedine.customerservice.dto.CustomerCreationQueueObject;
import com.finedine.customerservice.dto.CustomerResponse;
import com.finedine.customerservice.dto.CustomerUpdateRequest;
import com.finedine.customerservice.dto.OrderResponse;
import com.finedine.customerservice.entity.Customer;
import com.finedine.customerservice.exception.NotFoundException;
import com.finedine.customerservice.exception.UnauthorizedException;
import com.finedine.customerservice.repository.CustomerRepository;
import com.finedine.customerservice.security.SecurityUser;
import com.finedine.customerservice.util.CustomerMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static com.finedine.customerservice.util.CustomMessages.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    /**
     {@inheritDoc}
     */
    @SqsListener(value = "fds-customer-registration-queue.fifo")
    @Override
    public void createCustomer(CustomerCreationQueueObject data) {
        log.info("Received new customer data: {}", data);

        Customer customer = customerMapper.toCustomer(data);

        customerRepository.save(customer);
    }

    /**
     {@inheritDoc}
     */
    @Override
    public Customer myProfile(SecurityUser securityUser) {
        Customer customer = findByExternalId(securityUser);
        validateTenant(securityUser, customer);
        return customer;
    }

    /**
     {@inheritDoc}
     */
    @Override
    public CustomerResponse profileSettings(SecurityUser securityUser, CustomerUpdateRequest request) {

        Customer customer = findByExternalId(securityUser);

        validateTenant(securityUser, customer);

        customerMapper.updateCustomerFromRequest(request, customer);

        if (request.address() != null && !request.address().isBlank()) {
            if (customer.getAddress() == null) {
                customer.setAddress(new java.util.ArrayList<>());
            }
        }

        customerRepository.save(customer);

        return customerMapper.toCustomerResponse(customer);
    }

    /**
     {@inheritDoc}
     */
    @Override
    public Page<OrderResponse> getCustomerOrders(SecurityUser securityUser, Pageable pageable) {
        Customer customer = findByExternalId(securityUser);
        //rest api call to order service to get orders by customer externalId
        return null;
    }

    private Customer findByExternalId(SecurityUser securityUser) {
        return customerRepository.findByExternalId(securityUser.externalId())
                .orElseThrow(() -> new NotFoundException(CUSTOMER_NOT_FOUND));
    }

    private void validateTenant(SecurityUser securityUser, Customer customer) {
        if (securityUser.externalId() != null && !securityUser.externalId().equals(customer.getExternalId())) {
            throw new UnauthorizedException(RESTRICTED_ACTION);
        }
    }
}