package com.toggle.server.delivery.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

@Component
public class ToggleCallbackClient {

    private static final Logger log = LoggerFactory.getLogger(ToggleCallbackClient.class);

    private final RestClient restClient;

    public ToggleCallbackClient(RestClient deliveryRestClient) {
        this.restClient = deliveryRestClient;
    }

    public boolean deliver(String callbackUrl, String toggleName, ToggleCallbackPayload payload) {
        URI uri = URI.create(callbackUrl + "/" + toggleName);
        try {
            restClient.put()
                    .uri(uri)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.warn("Failed to deliver toggle update to {}: {}", uri, ex.getMessage());
            return false;
        }
    }
}
