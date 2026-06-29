package com.toggle.client.runtime;

public record ToggleResolution(
        String name,
        ConsumeMode mode,
        boolean found,
        boolean enabled,
        ToggleValue value,
        long version,
        String source) {

    public static ToggleResolution missing(String name, ConsumeMode mode, String source) {
        return new ToggleResolution(name, mode, false, false, null, 0L, source);
    }

    public static ToggleResolution fromSnapshot(ToggleSnapshot snapshot, ConsumeMode mode, String source) {
        return new ToggleResolution(
                snapshot.name(),
                mode,
                true,
                snapshot.enabled(),
                snapshot.value(),
                snapshot.version(),
                source);
    }
}
