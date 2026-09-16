package com.deals.adminservice.service;

import com.deals.adminservice.entity.Admin;
import com.deals.adminservice.repository.AdminRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    // Create admin
    public Admin createAdmin(Admin admin) {

        if (adminRepository.findByEmail(admin.getEmail()).isPresent()) {
            throw new RuntimeException("Admin email already exists");
        }

        if (admin.getStatus() == null) {
            admin.setStatus(Admin.AdminStatus.ACTIVE);
        }

        return adminRepository.save(admin);
    }

    // Get admin by ID
    public Admin getAdminById(Long id) {

        return adminRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Admin not found with ID: " + id));
    }

    // Get all admins
    public List<Admin> getAllAdmins() {

        return adminRepository.findAll();
    }

    // Find admin by email
    public Admin getAdminByEmail(String email) {

        return adminRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Admin not found with email: " + email));
    }

    // Update admin status
    public Admin updateStatus(Long id, Admin.AdminStatus status) {

        Admin admin = getAdminById(id);

        admin.setStatus(status);

        return adminRepository.save(admin);
    }

    // Delete admin
    public void deleteAdmin(Long id) {

        if (!adminRepository.existsById(id)) {
            throw new RuntimeException("Admin not found with ID: " + id);
        }

        adminRepository.deleteById(id);
    }
}