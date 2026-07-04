package com.toggle.server.toggle.web;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

public class UpdateToggleRequest {

    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    private String maintainer;
    private Boolean enabled;

    @Valid
    private ValueRequest value;
    private boolean valueExplicitlySet = false;

    public String getMaintainer() { return maintainer; }
    public void setMaintainer(String maintainer) { this.maintainer = maintainer; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public ValueRequest getValue() { return value; }

    @JsonSetter("value")
    public void setValue(ValueRequest value) {
        this.value = value;
        this.valueExplicitlySet = true;
    }

    public boolean isValueExplicitlySet() { return valueExplicitlySet; }
}
