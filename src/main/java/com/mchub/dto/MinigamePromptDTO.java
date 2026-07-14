package com.mchub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One line of text for the user to read out loud within the time limit. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinigamePromptDTO {
    private String text;
    /** Milliseconds allowed to read this line, based on word count + difficulty. */
    private int timeLimitMs;
    private int roundIndex;
}
