package com.toggle.server.client.domain;

public class ClientInstanceNotFoundException extends RuntimeException {

    public ClientInstanceNotFoundException(String serviceName, String instanceId) {
        super("Client instance '%s' not found for service '%s'".formatted(instanceId, serviceName));
    }
}
