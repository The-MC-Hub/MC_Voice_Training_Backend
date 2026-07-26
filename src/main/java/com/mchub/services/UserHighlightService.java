package com.mchub.services;

import com.mchub.models.UserHighlight;

import java.util.List;

public interface UserHighlightService {
    List<UserHighlight> getHighlights(String userId, String guideId);
    UserHighlight createHighlight(String userId, UserHighlight highlight);
    UserHighlight updateHighlight(String userId, String id, UserHighlight request);
    void deleteHighlight(String userId, String id);
}
