package com.toggle.server.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class OpenApiConfiguration {

    static final String OIDC_SCHEME_NAME = "oidc";
    static final String OIDC_PASSWORD_SCHEME_NAME = "oidc-password";

    @Bean
    public OpenAPI toggleServerOpenAPI(
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
    String tokenUrl = issuerUri + "/protocol/openid-connect/token";
        return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes(
                OIDC_SCHEME_NAME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.OPENIDCONNECT)
                    .openIdConnectUrl(issuerUri + "/.well-known/openid-configuration")
                    .description("Authenticate with Keycloak authorization code + PKCE."))
            .addSecuritySchemes(
                OIDC_PASSWORD_SCHEME_NAME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.OAUTH2)
                    .description("Authenticate with Keycloak using Resource Owner Password flow (local/dev).")
                    .flows(new OAuthFlows()
                        .password(new OAuthFlow().tokenUrl(tokenUrl)))))
        .addSecurityItem(new SecurityRequirement().addList(OIDC_SCHEME_NAME))
        .addSecurityItem(new SecurityRequirement().addList(OIDC_PASSWORD_SCHEME_NAME))
                .info(new Info()
                        .title("Toggle Server API")
                        .description("API to manage feature toggles and client registration")
                        .version("v1")
                        .contact(new Contact().name("Toggle Platform Team"))
                        .license(new License().name("Proprietary")));
    }
}
