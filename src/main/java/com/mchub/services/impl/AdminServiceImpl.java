package com.mchub.services.impl;

import com.mchub.dto.UserResponseDTO;
import com.mchub.enums.UserRole;
import com.mchub.models.User;
import com.mchub.repositories.UserRepository;
import com.mchub.mapper.UserMapper;
import com.mchub.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Map<String, Object> getAdminDashboardOverview() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMCs",         userRepository.countByRole(UserRole.MC));
        stats.put("totalUsers",       userRepository.count());
        return stats;
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
            .map(userMapper::toResponseDTO).toList();
    }

    @Override
    public List<UserResponseDTO> getAllMCs() {
        return userRepository.findByRole(UserRole.MC).stream()
            .map(userMapper::toResponseDTO).toList();
    }



    @Override
    public UserResponseDTO updateUserStatus(@NonNull String id, boolean isActive, boolean isVerified) {
        User user = userRepository.findById(Objects.requireNonNull(id))
            .orElseThrow(() -> new RuntimeException("User does not exist"));
        user.setActive(isActive);
        user.setVerified(isVerified);
        return userMapper.toResponseDTO(userRepository.save(user));
    }




}
