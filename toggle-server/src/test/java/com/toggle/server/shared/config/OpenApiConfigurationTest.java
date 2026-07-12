package com.toggle.server.shared.config;

import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigurationTest {

    private final OpenApiConfiguration configuration = new OpenApiConfiguration();

    @Test
    void shouldExposeOidcSecuritySchemeForSwaggerUiAuthentication() {
        var openApi = configuration.toggleServerOpenAPI("http://localhost:8089/realms/switchboard");

        assertThat(openApi.getComponents()).isNotNull();
        assertThat(openApi.getComponents().getSecuritySchemes())
                .containsKey(OpenApiConfiguration.OIDC_SCHEME_NAME);

        var scheme = openApi.getComponents().getSecuritySchemes().get(OpenApiConfiguration.OIDC_SCHEME_NAME);

        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.OPENIDCONNECT);
        assertThat(scheme.getOpenIdConnectUrl())
                .isEqualTo("http://localhost:8089/realms/switchboard/.well-known/openid-configuration");
    }

    @Test
    void shouldExposeOidcPasswordFlowUsingKeycloakTokenEndpoint() {
        var openApi = configuration.toggleServerOpenAPI("http://localhost:8089/realms/switchboard");

        var scheme = openApi.getComponents().getSecuritySchemes().get(OpenApiConfiguration.OIDC_PASSWORD_SCHEME_NAME);

        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.OAUTH2);
        assertThat(scheme.getFlows()).isNotNull();
        assertThat(scheme.getFlows().getPassword()).isNotNull();
        assertThat(scheme.getFlows().getPassword().getTokenUrl())
                .isEqualTo("http://localhost:8089/realms/switchboard/protocol/openid-connect/token");
    }

    @Test
    void shouldIncludeSecurityRequirementsForOidcAndPasswordFlows() {
        var openApi = configuration.toggleServerOpenAPI("http://localhost:8089/realms/switchboard");

        assertThat(openApi.getSecurity()).isNotNull();
        assertThat(openApi.getSecurity())
                .anySatisfy(requirement -> assertThat(requirement.containsKey(OpenApiConfiguration.OIDC_SCHEME_NAME)).isTrue())
                .anySatisfy(requirement -> assertThat(requirement.containsKey(OpenApiConfiguration.OIDC_PASSWORD_SCHEME_NAME)).isTrue());
    }
}