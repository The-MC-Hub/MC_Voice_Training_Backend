package com.mchub.services;

import com.mchub.dto.MinigameLeaderboardEntryDTO;
import com.mchub.dto.MinigamePromptDTO;
import com.mchub.dto.MinigameResultDTO;
import com.mchub.dto.MinigameSubmitRequest;

import java.util.List;

public interface MinigameService {
    List<MinigamePromptDTO> getSpeedReaderPrompts(String difficulty, int roundCount);
    MinigameResultDTO submitSpeedReaderRun(String userId, MinigameSubmitRequest request);
    List<MinigameLeaderboardEntryDTO> getLeaderboard(String gameType, int limit);
}
