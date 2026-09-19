package com.example.authservice.service;

import com.example.authservice.client.CustomerClient;
import com.example.authservice.client.MerchantClient;
import com.example.authservice.dto.*;
import com.example.authservice.entity.RefreshToken;
import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.exception.BadRequestException;
import com.example.authservice.repository.RefreshTokenRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomerClient customerClient;
    private final MerchantClient merchantClient;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       CustomerClient customerClient,
                       MerchantClient merchantClient) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.customerClient = customerClient;
        this.merchantClient = merchantClient;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID().toString().replace("-", ""),
                user,
                LocalDateTime.now().plusDays(7)
        );
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public AuthResponse registerCustomer(RegisterCustomerRequest request) {
        log.info("Registering new customer with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        User savedUser = userRepository.save(user);

        try {
            CustomerProfileDto profile = new CustomerProfileDto(
                    savedUser.getId(),
                    request.getName(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getPreferences()
            );
            customerClient.createCustomerProfile(profile);
            log.info("Customer profile synced successfully for user ID: {}", savedUser.getId());
        } catch (Exception e) {
            log.warn("Customer service sync failed or is running in mock mode: {}", e.getMessage());
        }

        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
        RefreshToken refreshToken = createRefreshToken(savedUser);
        return new AuthResponse(token, refreshToken.getToken(), savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getRole().name(), "Customer registered successfully");
    }

    @Transactional
    public AuthResponse registerMerchant(RegisterMerchantRequest request) {
        log.info("Registering new merchant with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getBusinessName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.MERCHANT);
        User savedUser = userRepository.save(user);

        try {
            MerchantProfileDto profile = new MerchantProfileDto(
                    savedUser.getId(),
                    request.getBusinessName(),
                    request.getOwnerName(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getCategory(),
                    request.getAddress(),
                    request.getDescription()
            );
            merchantClient.createMerchantProfile(profile);
            log.info("Merchant profile synced successfully for merchant ID: {}", savedUser.getId());
        } catch (Exception e) {
            log.warn("Merchant service sync failed or is running in mock mode: {}", e.getMessage());
        }

        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
        RefreshToken refreshToken = createRefreshToken(savedUser);
        return new AuthResponse(token, refreshToken.getToken(), savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getRole().name(), "Merchant registered successfully. Pending Admin approval.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password attempt for email: {}", request.getEmail());
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        RefreshToken refreshToken = createRefreshToken(user);
        log.info("User logged in successfully: {} (Role: {})", user.getEmail(), user.getRole());

        return new AuthResponse(token, refreshToken.getToken(), user.getId(), user.getName(), user.getEmail(), user.getRole().name(), "Login successful");
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Processing refresh token request");

        String reqToken = request.getRefreshToken();
        if (reqToken == null || reqToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(reqToken)
                .orElseThrow(() -> new BadRequestException("Invalid or non-existent refresh token"));

        if (refreshToken.isRevoked()) {
            throw new BadRequestException("Refresh token has been revoked. Please log in again.");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new BadRequestException("Refresh token has expired. Please log in again.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return new AuthResponse(
                newAccessToken,
                refreshToken.getToken(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                "Access token refreshed successfully"
        );
    }

    @Transactional
    public void logout(LogoutRequest request) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            refreshTokenRepository.findByToken(request.getRefreshToken()).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                log.info("Revoked refresh token on logout for user ID: {}", token.getUser().getId());
            });
        }
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found with email: " + request.getEmail()));

        String resetToken = UUID.randomUUID().toString().replace("-", "");
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        log.info("Generated password reset token for user: {}", user.getEmail());
        return new ForgotPasswordResponse(
                "Password reset token generated successfully. Valid for 15 minutes.",
                user.getEmail(),
                resetToken
        );
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset with provided token");

        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset token has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Password successfully reset for user: {}", user.getEmail());
        return new ResetPasswordResponse("Password has been reset successfully. You can now log in with your new password.");
    }
}
