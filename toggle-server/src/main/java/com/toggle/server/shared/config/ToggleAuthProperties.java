package com.toggle.server.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toggle.auth")
public class ToggleAuthProperties {

    private AuthMode mode = AuthMode.NONE;
    private final Oidc oidc = new Oidc();

    public AuthMode getMode() {
        return mode;
    }

    public void setMode(AuthMode mode) {
        this.mode = mode;
    }

    public Oidc getOidc() {
        return oidc;
    }

    public static class Oidc {
        private String clientId = "switchboard-admin-ui";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }
}
