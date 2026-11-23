package com.itzetech.dpdms.security;

import com.itzetech.dpdms.common.ApiException;
import com.itzetech.dpdms.config.AppProperties;
import com.itzetech.dpdms.user.UserAccount;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Issues and reads signed tokens. An access token (typ = "access") lets someone call the API.
 * A challenge token is short-lived and only proves one step of sign-in, for example
 * "password accepted, code still needed".
 */
@Service
public class TokenService {

    public static final String ACCESS = "access";
    public static final String MFA = "mfa";
    public static final String CHANGE_PASSWORD = "change-password";

    private final JwtEncoder encoder;
    private final JwtDecoder challengeDecoder;
    private final Duration accessTtl;

    public TokenService(AppProperties props) {
        byte[] secret = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters");
        }
        SecretKey key = new SecretKeySpec(secret, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.challengeDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.accessTtl = Duration.ofMinutes(props.jwt().accessTokenMinutes());
    }

    public String issueAccess(UserAccount u) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("dpdms").subject(u.getId().toString()).issuedAt(now).expiresAt(now.plus(accessTtl))
                .claim("typ", ACCESS).claim("role", u.getRole().name()).claim("name", u.getFullName())
                .build();
        return encode(claims);
    }

    public String issueChallenge(UUID userId, String purpose, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("dpdms").subject(userId.toString()).issuedAt(now).expiresAt(now.plus(ttl))
                .claim("typ", purpose)
                .build();
        return encode(claims);
    }

    /** Returns the user a challenge token was issued for, or fails if it is wrong, expired or for another step. */
    public UUID readChallenge(String token, String purpose) {
        try {
            Jwt jwt = challengeDecoder.decode(token);
            if (!purpose.equals(jwt.getClaimAsString("typ"))) {
                throw invalidChallenge();
            }
            return UUID.fromString(jwt.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw invalidChallenge();
        }
    }

    private ApiException invalidChallenge() {
        return ApiException.unauthorized("CHALLENGE_INVALID", "This step has expired. Please sign in again.");
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
