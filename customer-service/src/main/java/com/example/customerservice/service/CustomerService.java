package com.example.customerservice.service;

import com.example.customerservice.client.CashbackClient;
import com.example.customerservice.client.RedemptionClient;
import com.example.customerservice.dto.*;
import com.example.customerservice.entity.Customer;
import com.example.customerservice.entity.CustomerStatus;
import com.example.customerservice.exception.ResourceNotFoundException;
import com.example.customerservice.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final CashbackClient cashbackClient;
    private final RedemptionClient redemptionClient;

    public CustomerService(CustomerRepository customerRepository,
                           CashbackClient cashbackClient,
                           RedemptionClient redemptionClient) {
        this.customerRepository = customerRepository;
        this.cashbackClient = cashbackClient;
        this.redemptionClient = redemptionClient;
    }

    public CustomerResponse getCustomerById(Long id) {
        log.info("Fetching customer profile with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found with ID: " + id));

        return mapToResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found with ID: " + id));

        customer.setName(request.getName());
        customer.setPhone(request.getPhone());

        if (request.getPreferences() != null) {
            customer.setPreferences(request.getPreferences());
        }

        return mapToResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse updatePreferences(
            Long id,
            UpdatePreferencesRequest request) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found with ID: " + id));

        customer.setPreferences(request.getPreferences());

        return mapToResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerProfileDto createInternalCustomer(CustomerProfileDto dto) {

        if (customerRepository.existsById(dto.getId())) {
            return dto;
        }

        Customer customer = new Customer();

        customer.setId(dto.getId());
        customer.setName(dto.getName());
        customer.setEmail(dto.getEmail());
        customer.setPhone(dto.getPhone());
        customer.setPreferences(dto.getPreferences());
        customer.setStatus(CustomerStatus.ACTIVE);

        customerRepository.save(customer);

        return dto;
    }

    public WalletResponse getCustomerWallet(Long id) {

        ensureCustomerExists(id);

        try {
            return cashbackClient.getWalletByCustomerId(id);
        } catch (Exception e) {
            log.warn("Cashback service unavailable: {}", e.getMessage());

            return new WalletResponse(
                    null,
                    id,
                    java.math.BigDecimal.ZERO
            );
        }
    }

    public List<CashbackTransactionResponse> getCustomerTransactions(Long id) {

        ensureCustomerExists(id);

        try {
            return cashbackClient.getTransactionsByCustomerId(id);
        } catch (Exception e) {
            log.warn("Cashback service unavailable: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<RedemptionResponse> getCustomerRedemptions(Long id) {

        ensureCustomerExists(id);

        try {
            return redemptionClient.getRedemptionsByCustomerId(id);
        } catch (Exception e) {
            log.warn("Redemption service unavailable: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void ensureCustomerExists(Long id) {

        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Customer not found with ID: " + id);
        }
    }

    private CustomerResponse mapToResponse(Customer customer) {

        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getPreferences(),
                customer.getStatus(),
                customer.getCreatedAt()
        );
    }
}
