package com.mchub.services;

import java.util.List;
import java.util.Map;

public interface FavoriteService {

    Map<String, Object> toggle(String clientId, String mcUserId);

    List<String> getMyFavoriteIds(String clientId);

    boolean check(String clientId, String mcUserId);
}
