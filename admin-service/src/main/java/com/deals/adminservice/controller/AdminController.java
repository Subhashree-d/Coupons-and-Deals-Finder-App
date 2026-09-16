package com.deals.adminservice.controller;

import com.deals.adminservice.entity.Admin;
import com.deals.adminservice.service.AdminService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // Create Admin
    @PostMapping
    public Admin createAdmin(@RequestBody Admin admin) {
        return adminService.createAdmin(admin);
    }

    // Get all Admins
    @GetMapping
    public List<Admin> getAllAdmins() {
        return adminService.getAllAdmins();
    }

    // Get Admin by ID
    @GetMapping("/{id}")
    public Admin getAdminById(@PathVariable Long id) {
        return adminService.getAdminById(id);
    }

    // Get Admin by Email
    @GetMapping("/email/{email}")
    public Admin getAdminByEmail(@PathVariable String email) {
        return adminService.getAdminByEmail(email);
    }

    // Update Admin Status
    @PutMapping("/{id}/status")
    public Admin updateStatus(
            @PathVariable Long id,
            @RequestParam Admin.AdminStatus status) {

        return adminService.updateStatus(id, status);
    }

    // Delete Admin
    @DeleteMapping("/{id}")
    public String deleteAdmin(@PathVariable Long id) {

        adminService.deleteAdmin(id);

        return "Admin deleted successfully";
    }

    // Existing dashboard
    @GetMapping("/dashboard")
    public String dashboard() {
        return "Admin Dashboard";
    }

    // Existing merchant endpoints
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