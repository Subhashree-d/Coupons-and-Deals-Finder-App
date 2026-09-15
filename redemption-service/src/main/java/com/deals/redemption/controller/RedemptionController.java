package com.deals.redemption.controller;

import com.deals.redemption.entity.Redemption;
import com.deals.redemption.service.RedemptionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/redemptions")
public class RedemptionController {

    private final RedemptionService redemptionService;

    public RedemptionController(RedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    @PostMapping
    public ResponseEntity<Redemption> createRedemption(
            @Valid @RequestBody Redemption redemption) {

        Redemption savedRedemption =
                redemptionService.createRedemption(redemption);

        return new ResponseEntity<>(
                savedRedemption,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Redemption> getRedemptionById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                redemptionService.getRedemptionById(id)
        );
    }

    @GetMapping("/customer/{customerId}/redemptions")
    public ResponseEntity<List<Redemption>> getCustomerRedemptions(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                redemptionService.getRedemptionsByCustomer(customerId)
        );
    }

    @GetMapping("/merchant/{merchantId}/redemptions")
    public ResponseEntity<List<Redemption>> getMerchantRedemptions(
            @PathVariable Long merchantId) {

        return ResponseEntity.ok(
                redemptionService.getRedemptionsByMerchant(merchantId)
        );
    }
}