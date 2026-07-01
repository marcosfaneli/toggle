package com.toggle.server.delivery.infrastructure;

import com.toggle.server.shared.web.ToggleValueDto;

public record ToggleCallbackPayload(
        String name,
        String maintainer,
        boolean enabled,
        long version,
        ToggleValueDto value) {
}
