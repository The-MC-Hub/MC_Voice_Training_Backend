package com.mchub.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExerciseSubmitRequest {
    /**
     * MATCHING: pairs flattened as submitted ["left1","right1",...] in the order the user matched them
     * FILL_BLANK: words the user typed, in blank order
     * SENTENCE_ORDER: fragments in the order the user arranged them
     */
    private List<String> answer;
}
