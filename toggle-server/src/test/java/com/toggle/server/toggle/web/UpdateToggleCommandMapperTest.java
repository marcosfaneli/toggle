package com.toggle.server.toggle.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toggle.server.toggle.application.UpdateToggleCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateToggleCommandMapperTest {

    private final UpdateToggleCommandMapper mapper = new UpdateToggleCommandMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapToKeepWhenValueFieldAbsent() throws Exception {
        var request = objectMapper.readValue("{\"maintainer\":\"svc\",\"enabled\":true}", UpdateToggleRequest.class);

        UpdateToggleCommand command = mapper.toCommand("t", request);

        assertThat(command.valueUpdate()).isInstanceOf(UpdateToggleCommand.ValueUpdate.Keep.class);
        assertThat(command.maintainer()).isEqualTo("svc");
        assertThat(command.enabled()).isTrue();
    }

    @Test
    void shouldMapToRemoveWhenValueExplicitlyNull() throws Exception {
        var request = objectMapper.readValue("{\"maintainer\":\"svc\",\"value\":null}", UpdateToggleRequest.class);

        UpdateToggleCommand command = mapper.toCommand("t", request);

        assertThat(command.valueUpdate()).isInstanceOf(UpdateToggleCommand.ValueUpdate.Remove.class);
    }

    @Test
    void shouldMapToSetWhenValuePresent() throws Exception {
        var request = objectMapper.readValue(
            "{\"maintainer\":\"svc\",\"value\":{\"type\":\"NUMBER\",\"raw\":\"99\"}}", UpdateToggleRequest.class);

        UpdateToggleCommand command = mapper.toCommand("t", request);

        assertThat(command.valueUpdate()).isInstanceOf(UpdateToggleCommand.ValueUpdate.Set.class);
        var set = (UpdateToggleCommand.ValueUpdate.Set) command.valueUpdate();
        assertThat(set.type()).isEqualTo("NUMBER");
        assertThat(set.raw()).isEqualTo("99");
    }

    @Test
    void shouldMapNameAndMaintainer() throws Exception {
        var request = objectMapper.readValue("{\"maintainer\":\"checkout-service\"}", UpdateToggleRequest.class);

        UpdateToggleCommand command = mapper.toCommand("my-toggle", request);

        assertThat(command.name()).isEqualTo("my-toggle");
        assertThat(command.maintainer()).isEqualTo("checkout-service");
        assertThat(command.enabled()).isNull();
    }

    @Test
    void shouldAllowNullMaintainer() throws Exception {
        var request = objectMapper.readValue("{}", UpdateToggleRequest.class);

        UpdateToggleCommand command = mapper.toCommand("my-toggle", request);

        assertThat(command.name()).isEqualTo("my-toggle");
        assertThat(command.maintainer()).isNull();
        assertThat(command.enabled()).isNull();
    }
}
