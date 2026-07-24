package com.mchub.services.impl;

import com.mchub.models.Favorite;
import com.mchub.repositories.FavoriteRepository;
import com.mchub.services.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;

    @Override
    public Map<String, Object> toggle(String clientId, String mcUserId) {
        boolean exists = favoriteRepository.existsByClientIdAndMcUserId(clientId, mcUserId);
        Map<String, Object> result = new HashMap<>();
        if (exists) {
            favoriteRepository.deleteByClientIdAndMcUserId(clientId, mcUserId);
            result.put("favorited", false);
        } else {
            Favorite fav = Favorite.builder()
                    .clientId(clientId)
                    .mcUserId(mcUserId)
                    .build();
            favoriteRepository.save(fav);
            result.put("favorited", true);
        }
        return result;
    }

    @Override
    public List<String> getMyFavoriteIds(String clientId) {
        return favoriteRepository.findByClientId(clientId)
                .stream()
                .map(Favorite::getMcUserId)
                .collect(Collectors.toList());
    }

    @Override
    public boolean check(String clientId, String mcUserId) {
        return favoriteRepository.existsByClientIdAndMcUserId(clientId, mcUserId);
    }
}
