package com.toggle.client.web;

import com.toggle.client.runtime.ConsumeMode;
import com.toggle.client.runtime.ToggleCache;
import com.toggle.client.runtime.ToggleResolution;
import com.toggle.client.runtime.ToggleRuntimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalToggleController.class)
class InternalToggleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToggleRuntimeService runtimeService;

    @MockBean
    private ToggleCache toggleCache;

    @Test
    void shouldReturnNewFlowDecisionWhenToggleIsEnabled() throws Exception {
        when(runtimeService.resolve("pagamento-v2"))
                .thenReturn(new ToggleResolution("pagamento-v2", ConsumeMode.REMOTE_ALWAYS, true, true, null, 7L, "REMOTE"));

        mockMvc.perform(get("/internal/toggles/pagamento-v2/decision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toggleName").value("pagamento-v2"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.strategy").value("new-flow"))
                .andExpect(jsonPath("$.message").value("Usando fluxo novo"));
    }

    @Test
    void shouldReturnLegacyFlowDecisionWhenToggleIsDisabled() throws Exception {
        when(runtimeService.resolve("pagamento-v2"))
                .thenReturn(new ToggleResolution("pagamento-v2", ConsumeMode.LOCAL_CACHE, true, false, null, 1L, "CACHE"));

        mockMvc.perform(get("/internal/toggles/pagamento-v2/decision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toggleName").value("pagamento-v2"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.strategy").value("legacy-flow"))
                .andExpect(jsonPath("$.message").value("Usando fluxo legado"));
    }
}
