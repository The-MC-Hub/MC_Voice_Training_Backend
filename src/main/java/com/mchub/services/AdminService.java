package com.mchub.services;

import com.mchub.dto.UserResponseDTO;
import com.mchub.models.PaymentTransaction;

import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

public interface AdminService {

    @PreAuthorize("hasAuthority('ADMIN')")
    Map<String, Object> getAdminDashboardOverview();

    @PreAuthorize("hasAuthority('ADMIN')")
    List<UserResponseDTO> getAllUsers();

    @PreAuthorize("hasAuthority('ADMIN')")
    List<UserResponseDTO> getAllMCs();

    @PreAuthorize("hasAuthority('ADMIN')")
    List<Map<String, Object>> getAllTransactions();

    @PreAuthorize("hasAuthority('ADMIN')")
    Map<String, Object> getRevenueStats();

    @PreAuthorize("hasAuthority('ADMIN')")
    Map<String, Object> getAnalytics();

    @PreAuthorize("hasAuthority('ADMIN')")
    UserResponseDTO updateUserStatus(@NonNull String id, boolean isActive, boolean isVerified);

}
