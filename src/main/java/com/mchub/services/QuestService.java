package com.mchub.services;

import java.util.Map;

public interface QuestService {
    Map<String, Object> getProgress(String userId);
    Map<String, Object> completeQuest(String userId, String questId);
    Map<String, Object> claimVoucher(String userId);
}
