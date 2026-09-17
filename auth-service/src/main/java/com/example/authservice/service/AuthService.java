package com.example.authservice.service;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterCustomerRequest;
import com.example.authservice.dto.RegisterMerchantRequest;
import com.example.authservice.entity.User;
import com.example.authservice.entity.Role;
import com.example.authservice.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthResponse registerCustomer(RegisterCustomerRequest request) {
        // Customer registration logic will be added here
        return null;
    }

    public AuthResponse registerMerchant(RegisterMerchantRequest request) {
        // Merchant registration logic will be added here
        return null;
    }

    public AuthResponse login(LoginRequest request) {
        // Login and JWT logic will be added here
        return null;
    }
}
