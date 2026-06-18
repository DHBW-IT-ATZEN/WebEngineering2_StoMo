package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.model.entity.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}: a token must carry the user's email as subject and the
 * id as a {@code uid} claim, and must not validate under a different signing key.
 */
class JwtServiceTest {

    private static final String SECRET = "test-secret-0123456789-abcdefghij-klmnop";
    private static final SecretKey KEY = key(SECRET);

    @Test
    void issuesTokenWithEmailSubjectAndUidClaim() {
        String token = newService().issue(buildUser(42L, "jane@example.com"));

        Jwt decoded = decoderFor(KEY).decode(token);

        assertThat(decoded.getSubject()).isEqualTo("jane@example.com");
        assertThat(((Number) decoded.getClaim("uid")).longValue()).isEqualTo(42L);
        assertThat(decoded.getExpiresAt()).isNotNull();
    }

    @Test
    void tokenFromOneSecretDoesNotValidateUnderAnother() {
        String token = newService().issue(buildUser(1L, "jane@example.com"));
        JwtDecoder foreignDecoder = decoderFor(key("a-completely-different-secret-0123456789"));

        assertThatThrownBy(() -> foreignDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private static JwtService newService() {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(KEY));
        return new JwtService(encoder, 120);
    }

    private static JwtDecoder decoderFor(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static User buildUser(long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }
}
