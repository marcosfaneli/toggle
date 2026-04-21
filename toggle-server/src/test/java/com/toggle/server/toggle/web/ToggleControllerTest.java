package com.toggle.server.toggle.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toggle.server.toggle.application.CreateToggleCommand;
import com.toggle.server.toggle.application.CreateToggleUseCase;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.domain.ToggleAlreadyExistsException;
import com.toggle.server.toggle.domain.ToggleValue;
import com.toggle.server.toggle.domain.ValueType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ToggleController.class)
class ToggleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateToggleUseCase createToggleUseCase;

    @Test
    void shouldCreateToggleAndReturn201() throws Exception {
        var request = new CreateToggleRequest("novo-checkout", "checkout-service", true, null);
        var toggle = new Toggle("01ABCDEFGHIJKLMNOPQRSTUVWX", "novo-checkout", "checkout-service", true, 1L, LocalDateTime.now(), null);

        when(createToggleUseCase.execute(any(CreateToggleCommand.class))).thenReturn(toggle);

        var body = Objects.requireNonNull(objectMapper.writeValueAsString(request));

        mockMvc.perform(post("/toggles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("novo-checkout"))
                .andExpect(jsonPath("$.ownerServiceName").value("checkout-service"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.value").doesNotExist());
    }

    @Test
    void shouldCreateToggleWithStringValueAndReturn201() throws Exception {
        var valueRequest = new CreateToggleRequest.ValueRequest("STRING", "enabled");
        var request = new CreateToggleRequest("novo-checkout", "checkout-service", true, valueRequest);
        var toggleValue = new ToggleValue(ValueType.STRING, "enabled");
        var toggle = new Toggle("01ABCDEFGHIJKLMNOPQRSTUVWX", "novo-checkout", "checkout-service", true, 1L, LocalDateTime.now(), toggleValue);

        when(createToggleUseCase.execute(any(CreateToggleCommand.class))).thenReturn(toggle);

        var body = Objects.requireNonNull(objectMapper.writeValueAsString(request));

        mockMvc.perform(post("/toggles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value.type").value("STRING"))
                .andExpect(jsonPath("$.value.raw").value("enabled"));
    }

    @Test
    void shouldCreateToggleWithNumberValueAndReturn201() throws Exception {
        var valueRequest = new CreateToggleRequest.ValueRequest("NUMBER", "42");
        var request = new CreateToggleRequest("limite-requisicoes", "api-gateway", true, valueRequest);
        var toggleValue = new ToggleValue(ValueType.NUMBER, "42");
        var toggle = new Toggle("01ABCDEFGHIJKLMNOPQRSTUVXY", "limite-requisicoes", "api-gateway", true, 1L, LocalDateTime.now(), toggleValue);

        when(createToggleUseCase.execute(any(CreateToggleCommand.class))).thenReturn(toggle);

        var body = Objects.requireNonNull(objectMapper.writeValueAsString(request));

        mockMvc.perform(post("/toggles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value.type").value("NUMBER"))
                .andExpect(jsonPath("$.value.raw").value("42"));
    }

    @Test
    void shouldReturn400WhenValueTypeIsInvalid() throws Exception {
        var body = """
                {"name":"novo-checkout","ownerServiceName":"checkout-service","enabled":true,"value":{"type":"BOOLEAN","raw":"true"}}
                """;

        mockMvc.perform(post("/toggles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void shouldReturn400WhenValueTypeIsBlank() throws Exception {
        var body = """
                {"name":"novo-checkout","ownerServiceName":"checkout-service","enabled":true,"value":{"type":"","raw":"x"}}
                """;

        mockMvc.perform(post("/toggles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void shouldReturn400WhenValueRawIsBlank() throws Exception {
        var body = """
                {"name":"novo-checkout","ownerServiceName":"checkout-service","enabled":true,"value":{"type":"STRING","raw":""}}
                """;

        mockMvc.perform(post("/toggles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void shouldReturn409WhenToggleAlreadyExists() throws Exception {
        var request = new CreateToggleRequest("novo-checkout", "checkout-service", true, null);

        when(createToggleUseCase.execute(any(CreateToggleCommand.class)))
                .thenThrow(new ToggleAlreadyExistsException("novo-checkout", "checkout-service"));

        var body = Objects.requireNonNull(objectMapper.writeValueAsString(request));

        mockMvc.perform(post("/toggles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void shouldReturn400WhenNameIsBlank() throws Exception {
        var request = new CreateToggleRequest("", "checkout-service", true, null);
        var body = Objects.requireNonNull(objectMapper.writeValueAsString(request));

        mockMvc.perform(post("/toggles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void shouldReturn400WhenOwnerServiceNameIsBlank() throws Exception {
        var request = new CreateToggleRequest("novo-checkout", "", true, null);
        var body = Objects.requireNonNull(objectMapper.writeValueAsString(request));

        mockMvc.perform(post("/toggles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }
}
