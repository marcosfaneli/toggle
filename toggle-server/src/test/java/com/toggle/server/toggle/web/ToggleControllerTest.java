package com.toggle.server.toggle.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toggle.server.toggle.application.CreateToggleCommand;
import com.toggle.server.toggle.application.CreateToggleUseCase;
import com.toggle.server.toggle.application.ListTogglesQuery;
import com.toggle.server.toggle.application.ListTogglesUseCase;
import com.toggle.server.toggle.application.UpdateToggleCommand;
import com.toggle.server.toggle.application.UpdateToggleUseCase;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.domain.ToggleAlreadyExistsException;
import com.toggle.server.toggle.domain.ToggleNotFoundException;
import com.toggle.server.toggle.domain.ToggleValue;
import com.toggle.server.toggle.domain.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ToggleController.class)
@Import({CreateToggleCommandMapper.class, UpdateToggleCommandMapper.class, TogglePaginationLinkBuilder.class})
class ToggleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateToggleUseCase createToggleUseCase;

    @MockitoBean
    private ListTogglesUseCase listTogglesUseCase;

    @MockitoBean
    private UpdateToggleUseCase updateToggleUseCase;

    @Test
    void shouldCreateToggleAndReturn201() throws Exception {
        var request = new CreateToggleRequest("novo-checkout", "checkout-service", true, null);
        var toggle = new Toggle("01ABCDEFGHIJKLMNOPQRSTUVWX", "novo-checkout", "checkout-service", true, 1L, Instant.now(), null);

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
        var toggle = new Toggle("01ABCDEFGHIJKLMNOPQRSTUVWX", "novo-checkout", "checkout-service", true, 1L, Instant.now(), toggleValue);

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
        var toggle = new Toggle("01ABCDEFGHIJKLMNOPQRSTUVXY", "limite-requisicoes", "api-gateway", true, 1L, Instant.now(), toggleValue);

        when(createToggleUseCase.execute(any(CreateToggleCommand.class))).thenReturn(toggle);

        var body = Objects.requireNonNull(objectMapper.writeValueAsString(request));

        mockMvc.perform(post("/toggles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value.type").value("NUMBER"))
                .andExpect(jsonPath("$.value.raw").value("42"));
    }

    @ParameterizedTest
    @MethodSource("invalidValuePayloads")
    void shouldReturn400WhenValuePayloadIsInvalid(String body) throws Exception {
        mockMvc.perform(post("/toggles")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    private static Stream<String> invalidValuePayloads() {
        return Stream.of(
                """
                        {"name":"novo-checkout","ownerServiceName":"checkout-service","enabled":true,"value":{"type":"BOOLEAN","raw":"true"}}
                        """,
                """
                        {"name":"novo-checkout","ownerServiceName":"checkout-service","enabled":true,"value":{"type":"","raw":"x"}}
                        """,
                """
                        {"name":"novo-checkout","ownerServiceName":"checkout-service","enabled":true,"value":{"type":"STRING","raw":""}}
                        """
        );
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

    @Test
    void shouldReturnPagedTogglesAndLinkHeader() throws Exception {
        var toggle = new Toggle("01ID", "novo-checkout", "checkout-service", true, 1L, Instant.now(), null);
        var pageable = PageRequest.of(0, 20);
                var slice = newSlice(pageable, false, toggle);

        when(listTogglesUseCase.execute(any(ListTogglesQuery.class), any(Pageable.class))).thenReturn(slice);

        mockMvc.perform(get("/toggles")
                .param("ownerServiceName", "checkout-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("novo-checkout"))
                .andExpect(jsonPath("$.content[0].ownerServiceName").value("checkout-service"))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(header().string("Link", Objects.requireNonNull(org.hamcrest.Matchers.containsString("rel=\"first\""))));
    }

    @Test
    void shouldFilterByEnabledTrue() throws Exception {
        var toggle = new Toggle("01ID", "enabled-toggle", "checkout-service", true, 1L, Instant.now(), null);
        var pageable = PageRequest.of(0, 20);
                var slice = newSlice(pageable, false, toggle);

        when(listTogglesUseCase.execute(any(ListTogglesQuery.class), any(Pageable.class))).thenReturn(slice);

        mockMvc.perform(get("/toggles")
                .param("ownerServiceName", "checkout-service")
                .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].enabled").value(true));
    }

    @Test
    void shouldFilterByEnabledFalse() throws Exception {
        var toggle = new Toggle("01ID", "disabled-toggle", "checkout-service", false, 1L, Instant.now(), null);
        var pageable = PageRequest.of(0, 20);
                var slice = newSlice(pageable, false, toggle);

        when(listTogglesUseCase.execute(any(ListTogglesQuery.class), any(Pageable.class))).thenReturn(slice);

        mockMvc.perform(get("/toggles")
                .param("ownerServiceName", "checkout-service")
                .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].enabled").value(false));
    }

    @Test
    void shouldReturn400WhenEnabledParamIsInvalid() throws Exception {
        mockMvc.perform(get("/toggles")
                .param("ownerServiceName", "checkout-service")
                .param("enabled", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void shouldReturn400WhenOwnerServiceNameIsMissing() throws Exception {
        mockMvc.perform(get("/toggles"))
                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

        @Test
        void shouldReturn400WhenPageSizeIsTooLarge() throws Exception {
                mockMvc.perform(get("/toggles")
                                .param("ownerServiceName", "checkout-service")
                                .param("size", "101"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail").exists())
                                .andExpect(jsonPath("$.instance").exists());
        }

    @Test
    void shouldIncludeNextLinkWhenHasNextPage() throws Exception {
        var toggle = new Toggle("01ID", "toggle-a", "checkout-service", true, 1L, Instant.now(), null);
        var pageable = PageRequest.of(0, 20);
                var slice = newSlice(pageable, true, toggle);

        when(listTogglesUseCase.execute(any(ListTogglesQuery.class), any(Pageable.class))).thenReturn(slice);

        mockMvc.perform(get("/toggles")
                .param("ownerServiceName", "checkout-service"))
                .andExpect(status().isOk())
                .andExpect(header().string("Link", Objects.requireNonNull(org.hamcrest.Matchers.containsString("rel=\"next\""))));
    }

        @SuppressWarnings("null")
        private static Slice<Toggle> newSlice(Pageable pageable, boolean hasNext, Toggle... toggles) {
                return new SliceImpl<>(List.of(toggles), pageable, hasNext);
        }

    // --- H4 PATCH /toggles/{name} ---

    @Test
    void shouldUpdateEnabledAndReturn200() throws Exception {
        var toggle = new Toggle("01ID", "novo-checkout", "checkout-service", false, 2L, Instant.now(), null);
        when(updateToggleUseCase.execute(any(UpdateToggleCommand.class))).thenReturn(toggle);

        mockMvc.perform(patch("/toggles/novo-checkout")
                .param("ownerServiceName", "checkout-service")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void shouldUpdateValueAndReturn200() throws Exception {
        var toggleValue = new ToggleValue(ValueType.NUMBER, "99");
        var toggle = new Toggle("01ID", "limite", "api-gateway", true, 2L, Instant.now(), toggleValue);
        when(updateToggleUseCase.execute(any(UpdateToggleCommand.class))).thenReturn(toggle);

        mockMvc.perform(patch("/toggles/limite")
                .param("ownerServiceName", "api-gateway")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"value\":{\"type\":\"NUMBER\",\"raw\":\"99\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value.type").value("NUMBER"))
                .andExpect(jsonPath("$.value.raw").value("99"));
    }

    @Test
    void shouldRemoveValueAndReturn200() throws Exception {
        var toggle = new Toggle("01ID", "novo-checkout", "checkout-service", true, 2L, Instant.now(), null);
        when(updateToggleUseCase.execute(any(UpdateToggleCommand.class))).thenReturn(toggle);

        mockMvc.perform(patch("/toggles/novo-checkout")
                .param("ownerServiceName", "checkout-service")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"value\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").doesNotExist());
    }

    @Test
    void shouldKeepValueWhenValueFieldAbsentAndReturn200() throws Exception {
        var toggleValue = new ToggleValue(ValueType.STRING, "enabled");
        var toggle = new Toggle("01ID", "novo-checkout", "checkout-service", false, 2L, Instant.now(), toggleValue);
        when(updateToggleUseCase.execute(any(UpdateToggleCommand.class))).thenReturn(toggle);

        mockMvc.perform(patch("/toggles/novo-checkout")
                .param("ownerServiceName", "checkout-service")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value.type").value("STRING"))
                .andExpect(jsonPath("$.value.raw").value("enabled"));
    }

    @Test
    void shouldReturn404WhenToggleNotFound() throws Exception {
        when(updateToggleUseCase.execute(any(UpdateToggleCommand.class)))
                .thenThrow(new ToggleNotFoundException("inexistente", "checkout-service"));

        mockMvc.perform(patch("/toggles/inexistente")
                .param("ownerServiceName", "checkout-service")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"enabled\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    void shouldReturn400WhenPatchValueTypeIsInvalid() throws Exception {
        mockMvc.perform(patch("/toggles/novo-checkout")
                .param("ownerServiceName", "checkout-service")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{\"value\":{\"type\":\"BOOLEAN\",\"raw\":\"true\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_PROBLEM_JSON)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").exists());
    }
}
