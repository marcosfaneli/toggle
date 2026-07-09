package com.toggle.server.shared.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class KeycloakJwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String clientId;

    public KeycloakJwtRoleConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        var roles = new LinkedHashSet<String>();
        roles.addAll(rolesFromRealmAccess(jwt));
        roles.addAll(rolesFromResourceAccess(jwt));

        return roles.stream()
                .map(KeycloakJwtRoleConverter::toAuthority)
                .flatMap(Collection::stream)
                .toList();
    }

    private Set<String> rolesFromRealmAccess(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return Set.of();
        }
        return rolesFrom(realmAccess);
    }

    private Set<String> rolesFromResourceAccess(Jwt jwt) {
        var resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null) {
            return Set.of();
        }

        var clientAccess = resourceAccess.get(clientId);
        if (!(clientAccess instanceof Map<?, ?> claims)) {
            return Set.of();
        }

        return rolesFrom(claims);
    }

    private static Set<String> rolesFrom(Map<?, ?> claims) {
        var rawRoles = claims.get("roles");
        if (!(rawRoles instanceof Collection<?> values)) {
            return Set.of();
        }

        var roles = new LinkedHashSet<String>();
        for (var value : values) {
            if (value instanceof String role) {
                roles.add(role);
            }
        }
        return roles;
    }

    private static Collection<GrantedAuthority> toAuthority(String role) {
        return switch (role) {
            case "switchboard-admin" -> Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case "switchboard-maintainer" -> Set.of(new SimpleGrantedAuthority("ROLE_MAINTAINER"));
            case "switchboard-viewer" -> Set.of(new SimpleGrantedAuthority("ROLE_VIEWER"));
            default -> Set.of();
        };
    }
}
