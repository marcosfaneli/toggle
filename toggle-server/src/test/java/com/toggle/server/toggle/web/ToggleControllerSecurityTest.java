package com.toggle.server.toggle.web;

import com.toggle.server.shared.config.SecurityConfiguration;
import com.toggle.server.serviceauth.application.ServiceApiKeyService;
import com.toggle.server.toggle.application.CreateToggleCommand;
import com.toggle.server.toggle.application.CreateToggleUseCase;
import com.toggle.server.toggle.application.GetToggleByNameUseCase;
import com.toggle.server.toggle.application.ListToggleConsumersUseCase;
import com.toggle.server.toggle.application.ListTogglesUseCase;
import com.toggle.server.toggle.application.UpdateToggleUseCase;
import com.toggle.server.toggle.domain.Toggle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ToggleController.class)
@Import({
        SecurityConfiguration.class,
        CreateToggleCommandMapper.class,
        UpdateToggleCommandMapper.class,
        TogglePaginationLinkBuilder.class
})
@TestPropertySource(properties = "toggle.auth.mode=oidc")
class ToggleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ServiceApiKeyService serviceApiKeyService;

    @MockitoBean
    private CreateToggleUseCase createToggleUseCase;

    @MockitoBean
    private ListTogglesUseCase listTogglesUseCase;

    @MockitoBean
    private UpdateToggleUseCase updateToggleUseCase;

    @MockitoBean
    private GetToggleByNameUseCase getToggleByNameUseCase;

    @MockitoBean
    private ListToggleConsumersUseCase listToggleConsumersUseCase;

    @Test
    void shouldReturn401ForAdminEndpointWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/toggles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowViewerToReadToggle() throws Exception {
        when(getToggleByNameUseCase.execute("new-checkout"))
                .thenReturn(new Toggle(
                        "01TOGGLE0000000000000000000",
                        "new-checkout",
                        "checkout-service",
                        true,
                        1L,
                        Instant.parse("2026-05-10T12:00:00Z"),
                        null));

        mockMvc.perform(get("/toggles/new-checkout")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenViewerCreatesToggle() throws Exception {
        mockMvc.perform(post("/toggles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "new-checkout",
                                  "maintainer": "checkout-service",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowMaintainerToCreateToggle() throws Exception {
        when(createToggleUseCase.execute(any(CreateToggleCommand.class)))
                .thenReturn(new Toggle(
                        "01TOGGLE0000000000000000000",
                        "new-checkout",
                        "checkout-service",
                        true,
                        1L,
                        Instant.parse("2026-05-10T12:00:00Z"),
                        null));

        mockMvc.perform(post("/toggles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MAINTAINER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "new-checkout",
                                  "maintainer": "checkout-service",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isCreated());
    }

}
