package com.toggle.server.toggle.web;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateToggleRequest {

    private Boolean enabled;

    @Valid
    private ValueRequest value;
    private boolean valueExplicitlySet = false;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public ValueRequest getValue() { return value; }

    @JsonSetter("value")
    public void setValue(ValueRequest value) {
        this.value = value;
        this.valueExplicitlySet = true;
    }

    public boolean isValueExplicitlySet() { return valueExplicitlySet; }

    public record ValueRequest(
            @NotBlank @Pattern(regexp = "STRING|NUMBER", message = "must be STRING or NUMBER") String type,
            @NotBlank String raw) {}
}
