package com.toggle.client.web;

import com.toggle.client.runtime.ToggleCache;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureToggleCallbackControllerTest {

    @Test
    void shouldApplyCallbackUpdateToCache() {
        var cache = new ToggleCache();
        var controller = new FeatureToggleCallbackController(cache);
        var request = new InternalToggleUpdateRequest(
                "pagamento-v2",
                true,
                7L,
                Instant.parse("2026-04-21T17:00:00Z"),
                new InternalToggleUpdateRequest.ValueRequest("STRING", "variant-a"));

        var response = controller.updateToggle("pagamento-v2", request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(cache.get("pagamento-v2")).hasValueSatisfying(snapshot -> {
            assertThat(snapshot.enabled()).isTrue();
            assertThat(snapshot.version()).isEqualTo(7L);
            assertThat(snapshot.value().type()).isEqualTo("STRING");
            assertThat(snapshot.value().raw()).isEqualTo("variant-a");
        });
    }
}
