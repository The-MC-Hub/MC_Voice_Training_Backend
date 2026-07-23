package com.mchub.services;

import org.springframework.lang.NonNull;

/**
 * Verifies a Google Sign-In ID token (issued client-side by Google Identity Services)
 * and extracts the identity claims we trust.
 */
public interface GoogleTokenVerifierService {

    /**
     * @throws com.mchub.exception.AppException with ErrorCode.VALIDATION_FAILED if the token is
     *         invalid, expired, or was not issued for our configured client id.
     */
    GoogleIdentity verify(@NonNull String idToken);

    record GoogleIdentity(String googleId, String email, boolean emailVerified, String name, String pictureUrl) {
    }
}
