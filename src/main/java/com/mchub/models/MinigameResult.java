package com.mchub.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "minigame_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinigameResult {

    @Id
    private String id;

    private String userId;

    /** SPEED_READER | (future: EMOTION_MATCH, PAUSE_MASTER, MC_IMPRO) */
    private String gameType;

    /** Difficulty tier reached when the run ended */
    private String difficulty;

    /** Number of prompts read correctly before a miss/timeout ended the run */
    private int roundsCleared;

    /** Best combo (consecutive correct rounds) within this run */
    private int bestCombo;

    private int score;

    private double xpEarned;

    @CreatedDate
    private LocalDateTime createdAt;
}
