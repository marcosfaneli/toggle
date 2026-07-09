package com.toggle.server.serviceauth.application;

public record CreateServiceApiKeyCommand(String serviceName, String name) {
}
