package com.mchub.services.impl;

import com.mchub.models.UserVoucher;
import com.mchub.repositories.UserVoucherRepository;
import com.mchub.services.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final UserVoucherRepository userVoucherRepository;

    @Override
    public List<UserVoucher> getMyVouchers(String userId) {
        return userVoucherRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<UserVoucher> getAvailableVouchers(String userId) {
        return userVoucherRepository.findAvailableVouchers(userId, LocalDateTime.now());
    }
}
