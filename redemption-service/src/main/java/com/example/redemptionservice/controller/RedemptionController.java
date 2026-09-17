package com.example.redemptionservice.controller;

import com.example.redemptionservice.dto.CreateRedemptionRequest;
import com.example.redemptionservice.dto.RedemptionResponse;
import com.example.redemptionservice.service.RedemptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@Tag(name = "Redemption Controller", description = "Endpoints for coupon redemption and customer/merchant history")
public class RedemptionController {

    private final RedemptionService redemptionService;

    public RedemptionController(RedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    @PostMapping("/api/redemptions")
    @Operation(summary = "Customer redeems a coupon against a purchase")
    public ResponseEntity<RedemptionResponse> redeemCoupon(
            @Valid @RequestBody CreateRedemptionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String authUserId,
            @RequestHeader(value = "X-User-Role", required = false) String authUserRole) {
        return new ResponseEntity<>(redemptionService.redeemCoupon(request, authUserId, authUserRole), HttpStatus.CREATED);
    }

    @GetMapping("/api/redemptions/{id}")
    @Operation(summary = "Get redemption details by ID")
    public ResponseEntity<RedemptionResponse> getRedemptionById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(redemptionService.getRedemptionById(id));
    }

    @GetMapping("/api/customers/{id}/redemptions")
    @Operation(summary = "Get all redemptions by a customer")
    public ResponseEntity<List<RedemptionResponse>> getRedemptionsByCustomer(@PathVariable("id") Long id) {
        return ResponseEntity.ok(redemptionService.getRedemptionsByCustomerId(id));
    }

    @GetMapping("/api/merchants/{id}/redemptions")
    @Operation(summary = "Get all redemptions for a merchant")
    public ResponseEntity<List<RedemptionResponse>> getRedemptionsByMerchant(@PathVariable("id") Long id) {
        return ResponseEntity.ok(redemptionService.getRedemptionsByMerchantId(id));
    }
}
