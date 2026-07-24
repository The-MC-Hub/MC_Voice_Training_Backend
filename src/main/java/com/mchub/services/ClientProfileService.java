package com.mchub.services;

import com.mchub.dto.ClientProfileDTO;

public interface ClientProfileService {
    ClientProfileDTO getProfile(String userId);
    ClientProfileDTO updateProfile(String userId, ClientProfileDTO profileData);
    ClientProfileDTO createProfile(String userId, ClientProfileDTO profileData);
}
