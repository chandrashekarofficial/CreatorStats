package com.creatorhub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifierService {
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    private final String clientId;
    private final JwtDecoder decoder;

    public GoogleTokenVerifierService(@Value("${google.oauth.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        if (this.clientId.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID is not configured");
        }

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(GOOGLE_ISSUER);
        OAuth2TokenValidator<Jwt> audienceValidator = jwt ->
                jwt.getAudience().contains(this.clientId)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Google token audience does not match CreatorStats", null));

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        this.decoder = jwtDecoder;
    }

    public GoogleProfile verify(String credential) {
        try {
            Jwt jwt = decoder.decode(credential);
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            String subject = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            String name = jwt.getClaimAsString("name");
            String hostedDomain = jwt.getClaimAsString("hd");

            if (subject == null || subject.isBlank() || email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerified)) {
                throw new IllegalArgumentException("Google account email could not be verified");
            }

            return new GoogleProfile(subject, email.trim().toLowerCase(), name, hostedDomain);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid Google sign-in credential");
        }
    }

    public record GoogleProfile(String subject, String email, String name, String hostedDomain) {
        public boolean googleAuthoritativeEmail() {
            return email.endsWith("@gmail.com") || (hostedDomain != null && !hostedDomain.isBlank());
        }
    }
}
