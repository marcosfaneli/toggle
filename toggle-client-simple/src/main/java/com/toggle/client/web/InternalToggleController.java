package com.toggle.client.web;

import com.toggle.client.runtime.ConsumeMode;
import com.toggle.client.runtime.ToggleCache;
import com.toggle.client.runtime.ToggleResolution;
import com.toggle.client.runtime.ToggleRuntimeService;
import com.toggle.client.runtime.ToggleSnapshot;
import com.toggle.client.runtime.ToggleValue;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/internal")
public class InternalToggleController {

    private static final Logger log = LoggerFactory.getLogger(InternalToggleController.class);

    private final ToggleCache toggleCache;
    private final ToggleRuntimeService runtimeService;

    public InternalToggleController(ToggleCache toggleCache, ToggleRuntimeService runtimeService) {
        this.toggleCache = toggleCache;
        this.runtimeService = runtimeService;
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

    @GetMapping("/toggles/{toggleName}/resolve")
    public ToggleResolution resolve(@PathVariable String toggleName) {
        return runtimeService.resolve(toggleName);
    }

    @GetMapping("/toggles/{toggleName}/mode")
    public ConsumeMode mode(@PathVariable String toggleName) {
        var resolution = runtimeService.resolve(toggleName);
        return resolution.mode();
    }

    @GetMapping("/toggles/{toggleName}/decision")
    public ResponseEntity<ToggleDecisionResponse> decision(@PathVariable String toggleName) {
        var resolution = runtimeService.resolve(toggleName);
        var response = resolution.enabled()
                ? new ToggleDecisionResponse(toggleName, true, "new-flow", "Usando fluxo novo")
                : new ToggleDecisionResponse(toggleName, false, "legacy-flow", "Usando fluxo legado");

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
