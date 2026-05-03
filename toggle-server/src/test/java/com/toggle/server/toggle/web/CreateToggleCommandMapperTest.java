package com.toggle.server.toggle.web;

import com.toggle.server.toggle.application.CreateToggleCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateToggleCommandMapperTest {

    private final CreateToggleCommandMapper mapper = new CreateToggleCommandMapper();

    @Test
    void shouldMapRequestWithoutValue() {
        var request = new CreateToggleRequest("my-toggle", "svc", true, null);

        CreateToggleCommand command = mapper.toCommand(request);

        assertThat(command.name()).isEqualTo("my-toggle");
        assertThat(command.ownerServiceName()).isEqualTo("svc");
        assertThat(command.enabled()).isTrue();
        assertThat(command.value()).isNull();
    }

    @Test
    void shouldMapRequestWithStringValue() {
        var value = new CreateToggleRequest.ValueRequest("STRING", "active");
        var request = new CreateToggleRequest("my-toggle", "svc", false, value);

        CreateToggleCommand command = mapper.toCommand(request);

        assertThat(command.value()).isNotNull();
        assertThat(command.value().type()).isEqualTo("STRING");
        assertThat(command.value().raw()).isEqualTo("active");
    }

    @Test
    void shouldMapRequestWithNumberValue() {
        var value = new CreateToggleRequest.ValueRequest("NUMBER", "42");
        var request = new CreateToggleRequest("limit", "api-gw", true, value);

        CreateToggleCommand command = mapper.toCommand(request);

        assertThat(command.value().type()).isEqualTo("NUMBER");
        assertThat(command.value().raw()).isEqualTo("42");
    }
}
