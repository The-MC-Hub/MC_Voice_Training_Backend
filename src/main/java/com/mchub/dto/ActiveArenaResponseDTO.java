package com.mchub.dto;

import com.mchub.models.Competition;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
