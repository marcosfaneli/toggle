package com.toggle.client.example.web;

import com.toggle.client.runtime.FeatureToggleClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/checkout")
public class CheckoutDecisionController {

    private final FeatureToggleClient featureToggleClient;

    public CheckoutDecisionController(FeatureToggleClient featureToggleClient) {
        this.featureToggleClient = featureToggleClient;
    }

    @GetMapping("/toggles/{toggleName}/decision")
    public CheckoutDecisionResponse decision(@PathVariable String toggleName) {
        var resolution = featureToggleClient.resolve(toggleName);
        var value = Optional.ofNullable(resolution.value())
                .map(toggleValue -> new CheckoutDecisionResponse.ToggleValueResponse(
                        toggleValue.type(),
                        toggleValue.raw()))
                .orElse(null);

        return resolution.enabled()
                ? new CheckoutDecisionResponse(toggleName, true, "new-flow", value)
                : new CheckoutDecisionResponse(toggleName, false, "legacy-flow", value);
    }
}
