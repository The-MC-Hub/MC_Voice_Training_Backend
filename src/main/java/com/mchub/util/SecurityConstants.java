package com.mchub.util;

public final class SecurityConstants {

  private SecurityConstants() {
    // Utility class
  }

  public static final String[] DEFAULT_AVATAR_EMOJIS = {
    "🎤", "⭐", "👑", "🔥", "💎", "🚀", "🎵", "🏆", "✨", "⚡"
  };

  public static final String REFERRAL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  public static final int MAX_FAILED_ATTEMPTS = 10;
  public static final int LOCKOUT_MINUTES = 15;
  public static final int OTP_MAX_ATTEMPTS = 5;
  public static final int OTP_EXPIRATION_MINUTES = 10;
}
