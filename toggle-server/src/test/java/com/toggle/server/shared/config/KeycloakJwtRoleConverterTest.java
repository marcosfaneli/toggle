package com.toggle.server.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtRoleConverterTest {

    private final KeycloakJwtRoleConverter converter = new KeycloakJwtRoleConverter("switchboard-admin-ui");

    @Test
    void shouldMapRealmRolesToInternalAuthorities() {
        var jwt = jwt(Map.of("realm_access", Map.of("roles", List.of("switchboard-admin"))));

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldMapClientRolesToInternalAuthorities() {
        var jwt = jwt(Map.of(
                "resource_access",
                Map.of("switchboard-admin-ui", Map.of("roles", List.of("switchboard-maintainer", "switchboard-viewer")))));

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_MAINTAINER", "ROLE_VIEWER");
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
                "token",
                Instant.parse("2026-05-10T12:00:00Z"),
                Instant.parse("2026-05-10T12:05:00Z"),
                Map.of("alg", "none"),
                claims);
    }
}
