package com.toggle.server.serviceauth.web;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class ServiceApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final ServiceApiKeyPrincipal principal;

    public ServiceApiKeyAuthenticationToken(ServiceApiKeyPrincipal principal) {
        super(List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public ServiceApiKeyPrincipal getPrincipal() {
        return principal;
    }
}
