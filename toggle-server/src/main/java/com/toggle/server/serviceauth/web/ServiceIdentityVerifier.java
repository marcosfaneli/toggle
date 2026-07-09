package com.toggle.server.serviceauth.web;

import com.toggle.server.shared.config.AuthMode;
import com.toggle.server.shared.config.ToggleAuthProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ServiceIdentityVerifier {

    private final ToggleAuthProperties authProperties;

    public ServiceIdentityVerifier(ToggleAuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public void verify(Authentication authentication, String serviceName) {
        if (authProperties.getMode() == AuthMode.NONE) {
            return;
        }

        if (!(authentication instanceof ServiceApiKeyAuthenticationToken token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Service API key is required");
        }

        var authenticatedServiceName = token.getPrincipal().serviceName();
        if (!authenticatedServiceName.equals(serviceName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API key does not belong to serviceName");
        }
    }
}
