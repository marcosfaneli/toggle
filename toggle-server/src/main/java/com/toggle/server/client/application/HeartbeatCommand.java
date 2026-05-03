package com.toggle.server.client.application;

public record HeartbeatCommand(String serviceName, String instanceId) {
}
