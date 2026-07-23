package com.mchub.services;

import com.mchub.models.MCProfile;

public interface RecommendationService {

    /**
     * Fire-and-forget. Scans SearchInterest records for clients whose past search
     * matches this MC profile and sends a MC_RECOMMENDATION notification to each.
     * Called after an MC profile is created/updated and its status is AVAILABLE.
     */
    void notifyMatchingClients(MCProfile profile);

    /**
     * Scheduled sweep — re-checks all AVAILABLE MC profiles against all SearchInterest
     * records as a fallback for matches missed by the realtime trigger (e.g. an interest
     * saved after the MC profile already existed).
     */
    void runScheduledSweep();
}
