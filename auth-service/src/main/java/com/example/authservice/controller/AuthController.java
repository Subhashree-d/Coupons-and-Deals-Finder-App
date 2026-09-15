package com.example.authservice.controller;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterCustomerRequest;
import com.example.authservice.dto.RegisterMerchantRequest;
import com.example.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication Controller", description = "Endpoints for customer/merchant registration, login, and logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/customer")
    @Operation(summary = "Register a new Customer account")
    public ResponseEntity<AuthResponse> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
        AuthResponse response = authService.registerCustomer(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/register/merchant")
    @Operation(summary = "Register a new Merchant account (Status: PENDING)")
    public ResponseEntity<AuthResponse> registerMerchant(@Valid @RequestBody RegisterMerchantRequest request) {
        AuthResponse response = authService.registerMerchant(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with Email and Password to retrieve JWT Bearer token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user session")
    public ResponseEntity<Map<String, String>> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully. Please discard the JWT token client-side.");
        return ResponseEntity.ok(response);
    }
}
