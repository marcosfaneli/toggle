package com.toggle.server.serviceauth.web;

import com.toggle.server.serviceauth.application.ServiceApiKeyAuthenticationException;
import com.toggle.server.serviceauth.application.ServiceApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ServiceApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final ServiceApiKeyService serviceApiKeyService;

    public ServiceApiKeyAuthenticationFilter(ServiceApiKeyService serviceApiKeyService) {
        this.serviceApiKeyService = serviceApiKeyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var method = request.getMethod();
        var path = request.getServletPath();
        return !(
                ("POST".equals(method) && "/clients/register".equals(path))
                        || ("POST".equals(method) && "/clients/heartbeat".equals(path))
                        || ("DELETE".equals(method) && path.startsWith("/clients/register/"))
                        || ("GET".equals(method) && path.startsWith("/client-toggles/"))
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var rawKey = request.getHeader(API_KEY_HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var authentication = serviceApiKeyService.authenticate(rawKey);
            var principal = new ServiceApiKeyPrincipal(authentication.publicId(), authentication.serviceName());
            SecurityContextHolder.getContext().setAuthentication(new ServiceApiKeyAuthenticationToken(principal));
            filterChain.doFilter(request, response);
        } catch (ServiceApiKeyAuthenticationException exception) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "ApiKey");
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid service API key");
        }
    }
}
