package com.mchub.services.impl;

import com.mchub.dto.ClientProfileDTO;
import com.mchub.mapper.ClientProfileMapper;
import com.mchub.models.ClientProfile;
import com.mchub.models.User;
import com.mchub.repositories.ClientProfileRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.ClientProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class ClientProfileServiceImpl implements ClientProfileService {

    private final ClientProfileRepository clientProfileRepository;
    private final UserRepository userRepository;
    private final ClientProfileMapper clientProfileMapper;

    @Override
    public ClientProfileDTO getProfile(String userId) {
        ClientProfile profile = clientProfileRepository.findByUser(userId)
                .orElse(new ClientProfile());
        return clientProfileMapper.toDTO(profile);
    }

    @Override
    public ClientProfileDTO updateProfile(String userId, ClientProfileDTO profileData) {
        ClientProfile existing = clientProfileRepository.findByUser(userId)
                .orElse(ClientProfile.builder().user(userId).build());

        existing.setUser(userId);
        if (profileData.getRegion() != null) {
            existing.setRegion(profileData.getRegion());
        }
        if (profileData.getCustomRegion() != null) {
            existing.setCustomRegion(profileData.getCustomRegion());
        }
        if (profileData.getPreferredEventTypes() != null) {
            existing.setPreferredEventTypes(profileData.getPreferredEventTypes());
        }
        if (profileData.getOrganization() != null) {
            existing.setOrganization(profileData.getOrganization());
        }
        if (profileData.getBio() != null) {
            existing.setBio(profileData.getBio());
        }

        ClientProfile saved = clientProfileRepository.save(existing);
        return clientProfileMapper.toDTO(saved);
    }

    @Override
    public ClientProfileDTO createProfile(String userId, ClientProfileDTO profileData) {
        ClientProfile profile = ClientProfile.builder()
                .user(userId)
                .region(profileData.getRegion())
                .customRegion(profileData.getCustomRegion() != null ? profileData.getCustomRegion() : "")
                .preferredEventTypes(profileData.getPreferredEventTypes() != null ? profileData.getPreferredEventTypes() : new java.util.ArrayList<>())
                .organization(profileData.getOrganization() != null ? profileData.getOrganization() : "")
                .bio(profileData.getBio() != null ? profileData.getBio() : "")
                .build();

        ClientProfile saved = clientProfileRepository.save(profile);

        // Link profile to user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setClientProfile(saved.getId());
        userRepository.save(user);

        return clientProfileMapper.toDTO(saved);
    }
}
