package com.toggle.server.client.web;

import com.toggle.server.client.application.DeregisterClientUseCase;
import com.toggle.server.client.application.RegisterClientResult;
import com.toggle.server.client.application.RegisterClientUseCase;
import com.toggle.server.client.domain.ClientInstance;
import com.toggle.server.client.domain.ClientInstanceNotFoundException;
import com.toggle.server.client.domain.ClientInstanceStatus;
import com.toggle.server.client.domain.InvalidCallbackUrlException;
import com.toggle.server.client.domain.InvalidToggleSubscriptionException;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.domain.ToggleValue;
import com.toggle.server.toggle.domain.ValueType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.toggle.server.client.application.ClientView;
import com.toggle.server.client.application.ListClientsUseCase;
import com.toggle.server.serviceauth.web.ServiceIdentityVerifier;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RegisterClientCommandMapper.class})
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterClientUseCase registerClientUseCase;

    @MockitoBean
    private DeregisterClientUseCase deregisterClientUseCase;

    @MockitoBean
    private ListClientsUseCase listClientsUseCase;

    @MockitoBean
    private ServiceIdentityVerifier serviceIdentityVerifier;

    private static final Instant NOW = Instant.now();

    private static ClientInstance aClientInstance() {
        return new ClientInstance(
                "01INSTANCE00000000000000000",
                "checkout-service",
                "checkout-7d8d4c7f6f-abcde",
                "checkout-7d8d4c7f6f-abcde",
                "payments",
                "http://10.42.1.25:8080/internal/feature-toggles",
                ClientInstanceStatus.ACTIVE,
                NOW);
    }

    private static Toggle aToggle() {
        return new Toggle(
                "01TOGGLE0000000000000000000",
                "new-checkout",
                "checkout-service",
                true,
                1L,
                NOW,
                null);
    }

    private static ClientView aClientView() {
        return new ClientView(
                "01INSTANCE00000000000000000",
                "checkout-service",
                "checkout-7d8d4c7f6f-abcde",
                "checkout-7d8d4c7f6f-abcde",
                "payments",
                "http://10.42.1.25:8080/internal/feature-toggles",
                "ACTIVE",
                NOW,
                List.of(new ClientView.SubscriptionView("new-checkout", "LOCAL_CACHE")));
    }

    private static Toggle aToggleWithValue() {
        return new Toggle(
                "01TOGGLE0000000000000000001",
                "payment-v2",
                "checkout-service",
                true,
                2L,
                NOW,
                new ToggleValue(ValueType.STRING, "v2"));
    }

    @Test
    void shouldRegisterClientAndReturn200() throws Exception {
        var result = new RegisterClientResult(aClientInstance(), List.of(aToggle()));

        when(registerClientUseCase.execute(any())).thenReturn(result);

        var body = """
                {
                    "serviceName": "checkout-service",
                    "instanceId": "checkout-7d8d4c7f6f-abcde",
                    "podName": "checkout-7d8d4c7f6f-abcde",
                    "namespace": "payments",
                    "callbackUrl": "http://10.42.1.25:8080/internal/feature-toggles",
                    "subscriptions": [
                        { "toggleName": "new-checkout", "consumeMode": "LOCAL_CACHE" }
                    ]
                }
                """;

        mockMvc.perform(post("/clients/register")
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("checkout-service"))
                .andExpect(jsonPath("$.instanceId").value("checkout-7d8d4c7f6f-abcde"))
                .andExpect(jsonPath("$.toggles").isArray())
                .andExpect(jsonPath("$.toggles[0].name").value("new-checkout"))
                .andExpect(jsonPath("$.toggles[0].enabled").value(true))
                .andExpect(jsonPath("$.toggles[0].version").value(1))
                .andExpect(jsonPath("$.toggles[0].value").doesNotExist());
    }

    @Test
    void shouldReturnToggleValueInSnapshot() throws Exception {
        var result = new RegisterClientResult(aClientInstance(), List.of(aToggleWithValue()));

        when(registerClientUseCase.execute(any())).thenReturn(result);

        var body = """
                {
                    "serviceName": "checkout-service",
                    "instanceId": "checkout-7d8d4c7f6f-abcde",
                    "podName": "checkout-7d8d4c7f6f-abcde",
                    "namespace": "payments",
                    "callbackUrl": "http://10.42.1.25:8080/internal/feature-toggles",
                    "subscriptions": [
                        { "toggleName": "payment-v2", "consumeMode": "REMOTE_ALWAYS" }
                    ]
                }
                """;

        mockMvc.perform(post("/clients/register")
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toggles[0].value.type").value("STRING"))
                .andExpect(jsonPath("$.toggles[0].value.raw").value("v2"));
    }

    @Test
    void shouldReturn400WhenToggleNotFound() throws Exception {
        when(registerClientUseCase.execute(any()))
                .thenThrow(new InvalidToggleSubscriptionException(List.of("toggle-inexistente")));

        var body = """
                {
                    "serviceName": "checkout-service",
                    "instanceId": "checkout-7d8d4c7f6f-abcde",
                    "podName": "checkout-7d8d4c7f6f-abcde",
                    "namespace": "payments",
                    "callbackUrl": "http://10.42.1.25:8080/internal/feature-toggles",
                    "subscriptions": [
                        { "toggleName": "toggle-inexistente", "consumeMode": "LOCAL_CACHE" }
                    ]
                }
                """;

        mockMvc.perform(post("/clients/register")
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void shouldReturn400WhenCallbackUrlIsInvalid() throws Exception {
        when(registerClientUseCase.execute(any()))
                .thenThrow(new InvalidCallbackUrlException("Callback URL has no host"));

        var body = """
                {
                    "serviceName": "checkout-service",
                    "instanceId": "checkout-7d8d4c7f6f-abcde",
                    "podName": "checkout-7d8d4c7f6f-abcde",
                    "namespace": "payments",
                    "callbackUrl": "http:///invalid",
                    "subscriptions": [
                        { "toggleName": "new-checkout", "consumeMode": "LOCAL_CACHE" }
                    ]
                }
                """;

        mockMvc.perform(post("/clients/register")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Callback URL has no host"));
    }

    @Test
    void shouldReturn400WhenConsumeModeIsInvalid() throws Exception {
        var body = """
                {
                    "serviceName": "checkout-service",
                    "instanceId": "checkout-7d8d4c7f6f-abcde",
                    "podName": "checkout-7d8d4c7f6f-abcde",
                    "namespace": "payments",
                    "callbackUrl": "http://10.42.1.25:8080/internal/feature-toggles",
                    "subscriptions": [
                        { "toggleName": "new-checkout", "consumeMode": "INVALID_MODE" }
                    ]
                }
                """;

        mockMvc.perform(post("/clients/register")
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn400WhenRequiredFieldIsMissing() throws Exception {
        var body = """
                {
                    "instanceId": "checkout-7d8d4c7f6f-abcde",
                    "podName": "checkout-7d8d4c7f6f-abcde",
                    "namespace": "payments",
                    "callbackUrl": "http://10.42.1.25:8080/internal/feature-toggles",
                    "subscriptions": [
                        { "toggleName": "new-checkout", "consumeMode": "LOCAL_CACHE" }
                    ]
                }
                """;

        mockMvc.perform(post("/clients/register")
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)));
    }

    @Test
    void shouldReturn200WhenReRegisteringSameInstance() throws Exception {
        var result = new RegisterClientResult(aClientInstance(), List.of(aToggle()));
        when(registerClientUseCase.execute(any())).thenReturn(result);

        var body = """
                {
                    "serviceName": "checkout-service",
                    "instanceId": "checkout-7d8d4c7f6f-abcde",
                    "podName": "checkout-7d8d4c7f6f-abcde",
                    "namespace": "payments",
                    "callbackUrl": "http://10.42.1.25:8080/internal/feature-toggles",
                    "subscriptions": [
                        { "toggleName": "new-checkout", "consumeMode": "LOCAL_CACHE" }
                    ]
                }
                """;

        mockMvc.perform(post("/clients/register")
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("checkout-service"));
    }

    @Test
    void shouldListClientsWithSubscriptions() throws Exception {
        when(listClientsUseCase.execute(null, null)).thenReturn(List.of(aClientView()));

        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].instanceId").value("checkout-7d8d4c7f6f-abcde"))
                .andExpect(jsonPath("$[0].serviceName").value("checkout-service"))
                .andExpect(jsonPath("$[0].subscriptions").isArray())
                .andExpect(jsonPath("$[0].subscriptions[0].toggleName").value("new-checkout"))
                .andExpect(jsonPath("$[0].subscriptions[0].consumeMode").value("LOCAL_CACHE"));
    }

    @Test
    void shouldListClientsFilteredByServiceName() throws Exception {
        when(listClientsUseCase.execute("checkout-service", null)).thenReturn(List.of(aClientView()));

        mockMvc.perform(get("/clients")
                        .param("serviceName", "checkout-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].instanceId").value("checkout-7d8d4c7f6f-abcde"))
                .andExpect(jsonPath("$[0].serviceName").value("checkout-service"))
                .andExpect(jsonPath("$[0].subscriptions[0].toggleName").value("new-checkout"));
    }

    @Test
    void shouldListClientsFilteredByStatus() throws Exception {
        when(listClientsUseCase.execute(null, ClientInstanceStatus.ACTIVE)).thenReturn(List.of(aClientView()));

        mockMvc.perform(get("/clients")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].instanceId").value("checkout-7d8d4c7f6f-abcde"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void shouldListClientsFilteredByServiceNameAndStatus() throws Exception {
        when(listClientsUseCase.execute("checkout-service", ClientInstanceStatus.ACTIVE))
                .thenReturn(List.of(aClientView()));

        mockMvc.perform(get("/clients")
                        .param("serviceName", "checkout-service")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].serviceName").value("checkout-service"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void shouldReturnEmptyListWhenServiceNameHasNoClients() throws Exception {
        when(listClientsUseCase.execute("unknown-service", null)).thenReturn(List.of());

        mockMvc.perform(get("/clients")
                        .param("serviceName", "unknown-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturn400WhenServiceNameFilterIsBlank() throws Exception {
        mockMvc.perform(get("/clients")
                        .param("serviceName", " "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void shouldReturn400WhenStatusFilterIsInvalid() throws Exception {
        mockMvc.perform(get("/clients")
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void shouldDeregisterAndReturn204() throws Exception {
        doNothing().when(deregisterClientUseCase).execute(eq("checkout-service"), eq("checkout-7d8d4c7f6f-abcde"));

        mockMvc.perform(delete("/clients/register/{instanceId}", "checkout-7d8d4c7f6f-abcde")
                .param("serviceName", "checkout-service"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeregisteringUnknownInstance() throws Exception {
        doThrow(new ClientInstanceNotFoundException("checkout-service", "unknown-instance"))
                .when(deregisterClientUseCase).execute(any(), any());

        mockMvc.perform(delete("/clients/register/{instanceId}", "unknown-instance")
                .param("serviceName", "checkout-service"))
                .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }
}
