package com.toggle.server.toggle.web;

import com.toggle.server.toggle.application.CreateToggleCommand;
import org.springframework.stereotype.Component;

@Component
class CreateToggleCommandMapper {

    CreateToggleCommand toCommand(CreateToggleRequest request) {
        CreateToggleCommand.ToggleValueCommand valueCommand = null;
        if (request.value() != null) {
            valueCommand = new CreateToggleCommand.ToggleValueCommand(
                    request.value().type(),
                    request.value().raw());
        }
        return new CreateToggleCommand(
                request.name(),
                request.maintainer(),
                request.enabled(),
                valueCommand);
    }
}
