package com.toggle.server.shared.web;

import com.toggle.server.toggle.domain.ToggleValue;

public record ToggleValueDto(String type, String raw) {

    public static ToggleValueDto from(ToggleValue toggleValue) {
        if (toggleValue == null) return null;
        return new ToggleValueDto(toggleValue.type().name(), toggleValue.raw());
    }
}
