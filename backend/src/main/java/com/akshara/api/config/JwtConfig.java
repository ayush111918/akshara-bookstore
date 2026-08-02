package com.akshara.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    private static final int MINIMUM_SECRET_LENGTH_BYTES = 32;

    private final String jwtSecret;
    private final String jwtIssuer;

    public JwtConfig(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.issuer}") String jwtIssuer
    ) {
        this.jwtSecret = jwtSecret;
        this.jwtIssuer = jwtIssuer;
    }

    @Bean
    public SecretKey jwtSecretKey() {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < MINIMUM_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must contain at least 32 bytes for HS256"
            );
        }

        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder
                .withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(jwtIssuer)
        );

        return decoder;
    }
}