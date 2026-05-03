package com.toggle.server.toggle.web;

import com.toggle.server.toggle.application.UpdateToggleCommand;
import org.springframework.stereotype.Component;

@Component
class UpdateToggleCommandMapper {

    UpdateToggleCommand toCommand(String name, String ownerServiceName, UpdateToggleRequest request) {
        UpdateToggleCommand.ValueUpdate valueUpdate;
        if (!request.isValueExplicitlySet()) {
            valueUpdate = new UpdateToggleCommand.ValueUpdate.Keep();
        } else if (request.getValue() == null) {
            valueUpdate = new UpdateToggleCommand.ValueUpdate.Remove();
        } else {
            valueUpdate = new UpdateToggleCommand.ValueUpdate.Set(
                    request.getValue().type(),
                    request.getValue().raw());
        }
        return new UpdateToggleCommand(name, ownerServiceName, request.getEnabled(), valueUpdate);
    }
}
