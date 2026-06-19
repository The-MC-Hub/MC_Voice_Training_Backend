package com.mchub.config;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.VoiceLessonCategory;

/**
 * Central plan limits — single source of truth for all enforcement logic.
 */
public final class PlanConfig {

    private PlanConfig() {}

    public static final int DAILY_PRICE_VND  = 10_000;
    public static final int BASIC_PRICE_VND  = 199_000;
    public static final int FULL_PRICE_VND   = 299_000;
    public static final int ANNUAL_PRICE_VND = 1_990_000;

    public static final int DAILY_DAYS  = 1;
    public static final int BASIC_DAYS  = 30;
    public static final int FULL_DAYS   = 30;
    public static final int ANNUAL_DAYS = 365;

    // FREE tier: 5 total sessions, no AI coaching
    public static final int FREE_SESSION_LIMIT = 5;

    // DAILY tier: 10 AI coaching sessions per 24h period, all categories
    public static final int DAILY_AI_SESSION_LIMIT = 10;

    // BASIC tier: 20 AI coaching sessions/month, all categories allowed
    public static final int BASIC_AI_SESSION_LIMIT = 20;

    public static int priceFor(SubscriptionPlan plan) {
        return switch (plan) {
            case DAILY  -> DAILY_PRICE_VND;
            case BASIC  -> BASIC_PRICE_VND;
            case FULL   -> FULL_PRICE_VND;
            case ANNUAL -> ANNUAL_PRICE_VND;
            default     -> 0;
        };
    }

    public static int daysFor(SubscriptionPlan plan) {
        return switch (plan) {
            case DAILY  -> DAILY_DAYS;
            case BASIC  -> BASIC_DAYS;
            case FULL   -> FULL_DAYS;
            case ANNUAL -> ANNUAL_DAYS;
            default     -> 0;
        };
    }

    /** Returns true if the plan allows AI coaching (analyze-voice endpoint). */
    public static boolean allowsAiCoaching(SubscriptionPlan plan) {
        return plan == SubscriptionPlan.DAILY
            || plan == SubscriptionPlan.BASIC
            || plan == SubscriptionPlan.FULL
            || plan == SubscriptionPlan.ANNUAL;
    }

    /** Returns true if the plan can access lessons of the given category. */
    public static boolean allowsCategory(SubscriptionPlan plan, VoiceLessonCategory category) {
        return true; // All plans: categories allowed, session count enforced separately
    }
}
