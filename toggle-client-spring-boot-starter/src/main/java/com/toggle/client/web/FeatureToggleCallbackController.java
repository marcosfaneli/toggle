package com.toggle.client.web;

import com.toggle.client.runtime.ToggleCache;
import com.toggle.client.runtime.ToggleSnapshot;
import com.toggle.client.runtime.ToggleValue;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/internal")
public class FeatureToggleCallbackController {

    private static final Logger log = LoggerFactory.getLogger(FeatureToggleCallbackController.class);

    private final ToggleCache toggleCache;

    public FeatureToggleCallbackController(ToggleCache toggleCache) {
        this.toggleCache = toggleCache;
    }

    @PutMapping("/feature-toggles/{toggleName}")
    public ResponseEntity<Void> updateToggle(@PathVariable String toggleName,
                                             @Valid @RequestBody InternalToggleUpdateRequest request) {
        var snapshot = new ToggleSnapshot(
                toggleName,
                request.enabled(),
                request.value() == null ? null : new ToggleValue(request.value().type(), request.value().raw()),
                request.version(),
                request.updatedAt() == null ? Instant.now() : request.updatedAt());

        toggleCache.putIfNewer(snapshot);
        log.info("Applied callback update toggle={} version={} source=PUT", toggleName, request.version());
        return ResponseEntity.ok().build();
    }
}
