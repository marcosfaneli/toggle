package com.toggle.server.client.web;

import com.toggle.server.client.application.HeartbeatUseCase;
import com.toggle.server.client.domain.ClientInstanceInactiveException;
import com.toggle.server.client.domain.ClientInstanceNotFoundException;
import com.toggle.server.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HeartbeatController.class)
@Import(GlobalExceptionHandler.class)
class HeartbeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HeartbeatUseCase heartbeatUseCase;

    private static final String VALID_BODY = """
            {
              "serviceName": "checkout-service",
              "instanceId": "checkout-7d8d4c7f6f-abcde"
            }
            """;

    @Test
    void shouldReturn204OnSuccessfulHeartbeat() throws Exception {
        doNothing().when(heartbeatUseCase).execute(any());

        mockMvc.perform(post("/clients/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenInstanceNotFound() throws Exception {
        doThrow(new ClientInstanceNotFoundException("checkout-service", "checkout-7d8d4c7f6f-abcde"))
                .when(heartbeatUseCase).execute(any());

        mockMvc.perform(post("/clients/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void shouldReturn409WhenInstanceIsInactive() throws Exception {
        doThrow(new ClientInstanceInactiveException("checkout-service", "checkout-7d8d4c7f6f-abcde"))
                .when(heartbeatUseCase).execute(any());

        mockMvc.perform(post("/clients/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void shouldReturn400WhenServiceNameIsBlank() throws Exception {
        var body = """
                {
                  "serviceName": "",
                  "instanceId": "checkout-7d8d4c7f6f-abcde"
                }
                """;

        mockMvc.perform(post("/clients/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn400WhenInstanceIdIsBlank() throws Exception {
        var body = """
                {
                  "serviceName": "checkout-service",
                  "instanceId": ""
                }
                """;

        mockMvc.perform(post("/clients/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400));
    }
}
