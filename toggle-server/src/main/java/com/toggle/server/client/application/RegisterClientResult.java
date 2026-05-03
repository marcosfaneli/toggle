package com.toggle.server.client.application;

import com.toggle.server.client.domain.ClientInstance;
import com.toggle.server.toggle.domain.Toggle;

import java.util.List;

public record RegisterClientResult(ClientInstance instance, List<Toggle> toggleSnapshot) {}
