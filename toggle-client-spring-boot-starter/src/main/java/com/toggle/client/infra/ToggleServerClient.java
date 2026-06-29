package com.toggle.client.infra;

import com.toggle.client.runtime.ToggleSnapshot;

import java.util.List;
import java.util.Optional;

public interface ToggleServerClient {

    List<ToggleSnapshot> registerAndGetSnapshot();

    boolean isRegistered();

    void heartbeat();

    void deregister();

    Optional<ToggleSnapshot> fetchToggleByName(String toggleName);
}
