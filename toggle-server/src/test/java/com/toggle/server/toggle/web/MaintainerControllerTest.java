package com.toggle.server.toggle.web;

import com.toggle.server.toggle.application.ListMaintainersUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaintainerController.class)
class MaintainerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListMaintainersUseCase listMaintainersUseCase;

    @Test
    void shouldListMaintainers() throws Exception {
        when(listMaintainersUseCase.execute()).thenReturn(List.of("checkout-team", "platform-team"));

        mockMvc.perform(get("/maintainers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("checkout-team"))
                .andExpect(jsonPath("$[1]").value("platform-team"));
    }
}
