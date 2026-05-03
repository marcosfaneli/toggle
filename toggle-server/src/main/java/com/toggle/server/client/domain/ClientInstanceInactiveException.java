package com.toggle.server.client.domain;

public class ClientInstanceInactiveException extends RuntimeException {

    public ClientInstanceInactiveException(String serviceName, String instanceId) {
        super("Client instance '%s' for service '%s' is inactive".formatted(instanceId, serviceName));
    }
}
