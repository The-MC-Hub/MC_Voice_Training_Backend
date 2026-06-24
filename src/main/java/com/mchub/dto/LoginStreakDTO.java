package com.mchub.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class LoginStreakDTO {
    private int loginStreak;
    private int longestLoginStreak;
    private int freezesAvailable;
    private LocalDate lastLoginDate;
    /** Frame key: NONE | SPARK | FLAME | STORM | LEGEND | ELITE | IMMORTAL */
    private String streakFrame;
    /** Frame key for the next locked tier, null if already at max */
    private String nextFrame;
    /** Streak days needed to unlock next frame, 0 if at max */
    private int daysToNextFrame;
}
