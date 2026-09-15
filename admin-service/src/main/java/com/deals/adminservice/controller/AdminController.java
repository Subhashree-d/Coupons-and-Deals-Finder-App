package com.deals.adminservice.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "Admin Dashboard";
    }

    @GetMapping("/merchants")
    public String getMerchants() {
        return "List of merchants";
    }

    @PutMapping("/merchants/{id}/approve")
    public String approveMerchant(@PathVariable Long id) {
        return "Merchant " + id + " approved";
    }

    @PutMapping("/merchants/{id}/reject")
    public String rejectMerchant(@PathVariable Long id) {
        return "Merchant " + id + " rejected";
    }
}