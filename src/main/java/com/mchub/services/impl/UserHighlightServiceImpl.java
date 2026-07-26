package com.mchub.services.impl;

import com.mchub.exception.ErrorCode;
import com.mchub.models.UserHighlight;
import com.mchub.repositories.UserHighlightRepository;
import com.mchub.services.UserHighlightService;
import com.mchub.util.EntityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserHighlightServiceImpl implements UserHighlightService {

    private final UserHighlightRepository highlightRepository;

    @Override
    public List<UserHighlight> getHighlights(String userId, String guideId) {
        return highlightRepository.findByUserIdAndReadingGuideIdOrderByCreatedAtDesc(userId, guideId);
    }

    @Override
    public UserHighlight createHighlight(String userId, UserHighlight highlight) {
        highlight.setUserId(userId);
        highlight.setCreatedAt(new Date());
        highlight.setUpdatedAt(new Date());
        return highlightRepository.save(highlight);
    }

    @Override
    public UserHighlight updateHighlight(String userId, String id, UserHighlight request) {
        UserHighlight highlight = EntityUtils.getOrThrow(highlightRepository, id, ErrorCode.RESOURCE_NOT_FOUND, "Highlight not found: " + id);
        EntityUtils.verifyOwnership(highlight.getUserId(), userId);

        if (request.getColorHex() != null) highlight.setColorHex(request.getColorHex());
        if (request.getNoteContent() != null) highlight.setNoteContent(request.getNoteContent());
        highlight.setUpdatedAt(new Date());
        return highlightRepository.save(highlight);
    }

    @Override
    public void deleteHighlight(String userId, String id) {
        UserHighlight highlight = EntityUtils.getOrThrow(highlightRepository, id, ErrorCode.RESOURCE_NOT_FOUND, "Highlight not found: " + id);
        EntityUtils.verifyOwnership(highlight.getUserId(), userId);
        highlightRepository.deleteById(id);
    }
}
