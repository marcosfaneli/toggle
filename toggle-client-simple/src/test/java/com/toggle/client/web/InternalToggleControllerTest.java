package com.toggle.client.web;

import com.toggle.client.runtime.ConsumeMode;
import com.toggle.client.runtime.ToggleCache;
import com.toggle.client.runtime.ToggleResolution;
import com.toggle.client.runtime.ToggleRuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalToggleControllerTest {

    private MockMvc mockMvc;
    private StubToggleRuntimeService runtimeService;

    @BeforeEach
    void setUp() {
        runtimeService = new StubToggleRuntimeService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InternalToggleController(new ToggleCache(), runtimeService))
                .build();
    }

    @Test
    void shouldReturnNewFlowDecisionWhenToggleIsEnabled() throws Exception {
        runtimeService.resolution = new ToggleResolution("payment-v2", ConsumeMode.REMOTE_ALWAYS, true, true, null, 7L, "REMOTE");

        mockMvc.perform(get("/internal/toggles/payment-v2/decision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toggleName").value("payment-v2"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.strategy").value("new-flow"))
                .andExpect(jsonPath("$.message").value("Using new flow"));
    }

    @Test
    void shouldReturnLegacyFlowDecisionWhenToggleIsDisabled() throws Exception {
        runtimeService.resolution = new ToggleResolution("payment-v2", ConsumeMode.LOCAL_CACHE, true, false, null, 1L, "CACHE");

        mockMvc.perform(get("/internal/toggles/payment-v2/decision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toggleName").value("payment-v2"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.strategy").value("legacy-flow"))
                .andExpect(jsonPath("$.message").value("Using legacy flow"));
    }

    private static class StubToggleRuntimeService extends ToggleRuntimeService {

        private ToggleResolution resolution;

        private StubToggleRuntimeService() {
            super(null, null, null);
        }

        @Override
        public ToggleResolution resolve(String toggleName) {
            return resolution;
        }
    }
}
