package com.mchub.dto;

import com.mchub.models.Competition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveArenaResponseDTO {
    private Competition competition;
    private String challengeScriptTitle;
    private String challengeScriptContent;
    private List<ArenaLeaderboardEntryDTO> leaderboard;
    private ArenaLeaderboardEntryDTO userRecord;
}
