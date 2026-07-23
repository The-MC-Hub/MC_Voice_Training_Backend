package com.mchub.services.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.services.GoogleTokenVerifierService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.List;

@Slf4j
@Service
public class GoogleTokenVerifierServiceImpl implements GoogleTokenVerifierService {

    @Value("${mchub.google.client-id:}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    private void init() {
        if (googleClientId == null || googleClientId.isBlank()) {
            log.warn("⚠️ GOOGLE_CLIENT_ID not set — Google Sign-In endpoint will reject every request.");
            return;
        }
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(googleClientId))
                .build();
    }

    @Override
    public GoogleIdentity verify(@NonNull String idToken) {
        if (verifier == null) {
            throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID, "Google Sign-In is not configured on this server.");
        }
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID, "Invalid or expired Google token.");
            }
            GoogleIdToken.Payload payload = token.getPayload();
            String email = payload.getEmail();
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            if (email == null || email.isBlank()) {
                throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID, "Google token did not include an email.");
            }
            if (!emailVerified) {
                throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID, "Google account email is not verified.");
            }

            return new GoogleIdentity(payload.getSubject(), email, true, name, picture);
        } catch (AppException e) {
            throw e;
        } catch (GeneralSecurityException | java.io.IOException | IllegalArgumentException e) {
            log.warn("⚠️ [Google] Token verification failed: {}", e.getMessage());
            throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID, "Invalid or expired Google token.");
        }
    }
}
