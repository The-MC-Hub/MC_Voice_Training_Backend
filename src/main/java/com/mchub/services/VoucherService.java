package com.mchub.services;

import com.mchub.models.UserVoucher;

import java.util.List;

public interface VoucherService {
    List<UserVoucher> getMyVouchers(String userId);
    List<UserVoucher> getAvailableVouchers(String userId);
}
