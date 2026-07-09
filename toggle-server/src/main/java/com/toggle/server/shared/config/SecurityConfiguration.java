package com.toggle.server.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import com.toggle.server.serviceauth.application.ServiceApiKeyService;
import com.toggle.server.serviceauth.web.ServiceApiKeyAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ToggleAuthProperties.class)
public class SecurityConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "toggle.auth", name = "mode", havingValue = "none", matchIfMissing = true)
    SecurityFilterChain noAuthSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "toggle.auth", name = "mode", havingValue = "oidc")
    SecurityFilterChain oidcSecurityFilterChain(
            HttpSecurity http,
            ToggleAuthProperties authProperties,
            ServiceApiKeyService serviceApiKeyService) throws Exception {
        var jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
                new KeycloakJwtRoleConverter(authProperties.getOidc().getClientId()));

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/toggles").hasAnyRole("ADMIN", "MAINTAINER", "VIEWER")
                        .requestMatchers(HttpMethod.GET, "/toggles/*").hasAnyRole("ADMIN", "MAINTAINER", "VIEWER")
                        .requestMatchers(HttpMethod.GET, "/toggles/*/clients").hasAnyRole("ADMIN", "MAINTAINER", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/toggles").hasAnyRole("ADMIN", "MAINTAINER")
                        .requestMatchers(HttpMethod.PATCH, "/toggles/*").hasAnyRole("ADMIN", "MAINTAINER")
                        .requestMatchers(HttpMethod.GET, "/clients").hasAnyRole("ADMIN", "MAINTAINER", "VIEWER")
                        .requestMatchers(HttpMethod.GET, "/service-api-keys").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/service-api-keys").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/service-api-keys/*").hasRole("ADMIN")
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/clients/register").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.POST, "/clients/heartbeat").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.DELETE, "/clients/register/*").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.GET, "/client-toggles/*").hasRole("SERVICE")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .addFilterBefore(new ServiceApiKeyAuthenticationFilter(serviceApiKeyService), BearerTokenAuthenticationFilter.class)
                .build();
    }
}
